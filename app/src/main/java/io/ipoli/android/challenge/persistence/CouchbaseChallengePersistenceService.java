package io.ipoli.android.challenge.persistence;

import android.os.Handler;
import android.os.Looper;
import android.util.Pair;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Database;
import com.couchbase.lite.LiveQuery;
import com.couchbase.lite.Query;
import com.couchbase.lite.QueryEnumerator;
import com.couchbase.lite.QueryRow;
import com.couchbase.lite.View;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.ipoli.android.app.persistence.BaseCouchbasePersistenceService;
import io.ipoli.android.challenge.data.Challenge;
import io.ipoli.android.quest.data.Quest;
import io.ipoli.android.quest.data.QuestData;
import io.ipoli.android.quest.data.RepeatingQuest;
import io.ipoli.android.quest.persistence.OnDataChangedListener;
import io.ipoli.android.quest.persistence.QuestPersistenceService;
import io.ipoli.android.quest.persistence.RepeatingQuestPersistenceService;

/**
 * Created by Venelin Valkov <venelin@curiousily.com>
 * on 3/5/17.
 */
public class CouchbaseChallengePersistenceService extends BaseCouchbasePersistenceService<Challenge> implements ChallengePersistenceService {

    private final View allChallengesView;
    private final View challengesWithAllQuestsView;
    private final QuestPersistenceService questPersistenceService;
    private final RepeatingQuestPersistenceService repeatingQuestPersistenceService;
    private final View allQuestsAndRepeatingQuestsForChallengeView;

    public CouchbaseChallengePersistenceService(Database database, ObjectMapper objectMapper, QuestPersistenceService questPersistenceService, RepeatingQuestPersistenceService repeatingQuestPersistenceService) {
        super(database, objectMapper);

        this.questPersistenceService = questPersistenceService;
        this.repeatingQuestPersistenceService = repeatingQuestPersistenceService;

        allChallengesView = database.getView("challenges/all");
        if (allChallengesView.getMap() == null) {
            allChallengesView.setMap((document, emitter) -> {
                String type = (String) document.get("type");
                if (Challenge.TYPE.equals(type)) {
                    emitter.emit(document.get("_id"), document);
                }
            }, "1.0");
        }

        challengesWithAllQuestsView = database.getView("challenges/withAllQuests");
        if (challengesWithAllQuestsView.getMap() == null) {
            challengesWithAllQuestsView.setMapReduce((document, emitter) -> {
                String type = (String) document.get("type");
                if (Challenge.TYPE.equals(type)) {
                    emitter.emit(document.get("_id"), document);
                } else if (document.containsKey("challengeId") &&
                        (RepeatingQuest.TYPE.equals(type) || Quest.TYPE.equals(type))) {
                    emitter.emit(document.get("challengeId"), document);
                }
            }, (keys, values, rereduce) -> {
                Challenge challenge = null;
                List<RepeatingQuest> repeatingQuests = new ArrayList<>();
                List<Quest> quests = new ArrayList<>();

                for (Object v : values) {
                    Map<String, Object> data = (Map<String, Object>) v;
                    String type = (String) data.get("type");
                    if (Challenge.TYPE.equals(type)) {
                        challenge = toObject(data);
                    } else if (RepeatingQuest.TYPE.equals(type)) {
                        repeatingQuests.add(toObject(data, RepeatingQuest.class));
                    } else {
                        quests.add(toObject(data, Quest.class));
                    }
                }
                return new Pair<>(challenge, new Pair<>(repeatingQuests, quests));
            }, "1.0");
        }

        allQuestsAndRepeatingQuestsForChallengeView = database.getView("challenges/allQuestsAndRepeatingQuests");
        if (allQuestsAndRepeatingQuestsForChallengeView.getMap() == null) {
            allQuestsAndRepeatingQuestsForChallengeView.setMap((document, emitter) -> {
                String type = (String) document.get("type");
                if ((Quest.TYPE.equals(type) && !document.containsKey("completedAt") && !document.containsKey("repeatingQuestId")) ||
                        (RepeatingQuest.TYPE.equals(type) && !document.containsKey("completedAt"))) {
                    emitter.emit(document.get("challengeId"), document);
                }
            }, "1.0");
        }
    }

    @Override
    public void listenById(String id, OnDataChangedListener<Challenge> listener) {
        LiveQuery query = challengesWithAllQuestsView.createQuery().toLiveQuery();
        query.setStartKey(id);
        query.setEndKey(id);
        query.setGroupLevel(1);
        LiveQuery.ChangeListener changeListener = event -> {
            if (event.getSource().equals(query)) {
                Challenge ch = null;

                QueryEnumerator enumerator = event.getRows();
                while (enumerator.hasNext()) {
                    QueryRow row = enumerator.next();
                    Pair<Challenge, Pair<List<RepeatingQuest>, List<Quest>>> pair = (Pair<Challenge, Pair<List<RepeatingQuest>, List<Quest>>>) row.getValue();
                    ch = pair.first;
                    List<RepeatingQuest> repeatingQuests = pair.second.first;
                    List<Quest> quests = pair.second.second;

                    for (RepeatingQuest rq : repeatingQuests) {
                        ch.addChallengeRepeatingQuest(rq);
                    }

                    for (Quest q : quests) {
                        if (!q.isFromRepeatingQuest()) {
                            ch.addChallengeQuest(q);
                        }
                        ch.addQuestData(q.getId(), new QuestData(q));
                    }
                }
                final Challenge result = ch;
                new Handler(Looper.getMainLooper()).post(() -> listener.onDataChanged(result));
            }
        };
        startLiveQuery(query, changeListener);
    }

    @Override
    public void listenForAll(OnDataChangedListener<List<Challenge>> listener) {
        LiveQuery query = allChallengesView.createQuery().toLiveQuery();
        LiveQuery.ChangeListener changeListener = event -> {
            if (event.getSource().equals(query)) {
                List<Challenge> result = new ArrayList<>();
                QueryEnumerator enumerator = event.getRows();
                while (enumerator.hasNext()) {
                    result.add(toObject(enumerator.next().getValue()));
                }
                new Handler(Looper.getMainLooper()).post(() -> listener.onDataChanged(result));
            }
        };
        startLiveQuery(query, changeListener);
    }

    @Override
    public void delete(Challenge object) {
        delete(object, false);
    }

    @Override
    public void delete(Challenge challenge, boolean deleteWithQuests) {
        database.runInTransaction(() -> {

            Query query = challengesWithAllQuestsView.createQuery();
            query.setStartKey(challenge.getId());
            query.setEndKey(challenge.getId());
            query.setGroupLevel(1);
            try {
                QueryEnumerator enumerator = query.run();
                while (enumerator.hasNext()) {
                    QueryRow row = enumerator.next();
                    Pair<Challenge, Pair<List<RepeatingQuest>, List<Quest>>> pair = (Pair<Challenge, Pair<List<RepeatingQuest>, List<Quest>>>) row.getValue();
                    List<RepeatingQuest> repeatingQuests = pair.second.first;
                    List<Quest> quests = pair.second.second;
                    if (deleteWithQuests) {
                        deleteQuestsForChallenge(repeatingQuests, quests);
                    } else {
                        removeChallengeIdFromQuests(repeatingQuests, quests);
                    }
                    CouchbaseChallengePersistenceService.super.delete(pair.first);
                }
                return true;
            } catch (CouchbaseLiteException e) {
                return false;
            }
        });
    }

    @Override
    public void findAllQuestsAndRepeatingQuestsNotForChallenge(String searchQuery, Challenge challenge, OnDataChangedListener<Pair<List<RepeatingQuest>, List<Quest>>> listener) {
        try {
            QueryEnumerator enumerator = allQuestsAndRepeatingQuestsForChallengeView.createQuery().run();
            List<RepeatingQuest> repeatingQuests = new ArrayList<>();
            List<Quest> quests = new ArrayList<>();
            while (enumerator.hasNext()) {
                Map<String, Object> value = (Map<String, Object>) enumerator.next().getValue();
                String name = value.get("name").toString().toLowerCase();
                String challengeId = (String) value.get("challengeId");
                if(challenge.getId().equals(challengeId) || !name.contains(searchQuery.toLowerCase())) {
                    continue;
                }

                if (RepeatingQuest.TYPE.equals(value.get("type"))) {
                    repeatingQuests.add(toObject(value, RepeatingQuest.class));
                } else {
                    quests.add(toObject(value, Quest.class));
                }
            }
            new Handler(Looper.getMainLooper()).post(() -> listener.onDataChanged(new Pair<>(repeatingQuests, quests)));
        } catch (CouchbaseLiteException e) {
            e.printStackTrace();
        }
    }

    private void removeChallengeIdFromQuests(List<RepeatingQuest> repeatingQuests, List<Quest> quests) {
        for (Quest q : quests) {
            q.setChallengeId(null);
            questPersistenceService.save(q);
        }
        for (RepeatingQuest rq : repeatingQuests) {
            rq.setChallengeId(null);
            repeatingQuestPersistenceService.save(rq);
        }
    }

    private void deleteQuestsForChallenge(List<RepeatingQuest> repeatingQuests, List<Quest> quests) {
        for (Quest q : quests) {
            if (q.isCompleted()) {
                q.setRepeatingQuestId(null);
                q.setChallengeId(null);
                questPersistenceService.save(q);
            } else {
                questPersistenceService.delete(q);
            }
        }
        for (RepeatingQuest rq : repeatingQuests) {
            repeatingQuestPersistenceService.delete(rq);
        }
    }

    @Override
    protected Class<Challenge> getModelClass() {
        return Challenge.class;
    }
}
