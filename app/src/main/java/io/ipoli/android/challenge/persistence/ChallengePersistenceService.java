package io.ipoli.android.challenge.persistence;

import android.util.Pair;

import java.util.List;

import io.ipoli.android.app.persistence.PersistenceService;
import io.ipoli.android.challenge.data.Challenge;
import io.ipoli.android.quest.data.Quest;
import io.ipoli.android.quest.data.RepeatingQuest;
import io.ipoli.android.quest.persistence.OnDataChangedListener;

/**
 * Created by Venelin Valkov <venelin@curiousily.com>
 * on 6/24/16.
 */
public interface ChallengePersistenceService extends PersistenceService<Challenge> {
    void listenForAll(OnDataChangedListener<List<Challenge>> listener);

    void delete(Challenge challenge, boolean deleteWithQuests);

    void findAllQuestsAndRepeatingQuestsNotForChallenge(String query, Challenge challenge, OnDataChangedListener<Pair<List<RepeatingQuest>, List<Quest>>> listener);
}
