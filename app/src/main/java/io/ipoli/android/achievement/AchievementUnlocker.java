package io.ipoli.android.achievement;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by Venelin Valkov <venelin@curiousily.com>
 * on 7/24/17.
 */
public class AchievementUnlocker {

    private final Map<Achievement, AchievementChecker> achievementToChecker = new HashMap<>();

    public AchievementUnlocker() {
        achievementToChecker.put(Achievement.FIRST_QUEST_COMPLETED, new FirstQuestCompletedChecker());
        achievementToChecker.put(Achievement.COMPLETE_10_QUESTS_IN_A_DAY, new Complete10QuestInADayChecker());
    }

    @NonNull
    public List<Achievement> findUnlocked(Set<Integer> unlockedAchievementCodes, AchievementsProgress progress) {
        List<Achievement> achievementsToUnlock = new ArrayList<>();
        for (Achievement achievement : Achievement.values()) {
            if (unlockedAchievementCodes.contains(achievement.code)) {
                continue;
            }
            addAchievementIfUnlocked(progress, achievementsToUnlock, achievement);
        }
        return achievementsToUnlock;
    }

    private void addAchievementIfUnlocked(AchievementsProgress progress, List<Achievement> achievementsToUnlock, Achievement achievement) {
        if (achievementToChecker.get(achievement).shouldUnlock(progress)) {
            achievementsToUnlock.add(achievement);
        }
    }

    interface AchievementChecker {
        boolean shouldUnlock(AchievementsProgress progress);
    }

    class FirstQuestCompletedChecker implements AchievementChecker {

        @Override
        public boolean shouldUnlock(AchievementsProgress progress) {
            return progress.getCompletedQuestCount() == 1;
        }
    }

    private class Complete10QuestInADayChecker implements AchievementChecker {
        @Override
        public boolean shouldUnlock(AchievementsProgress progress) {
            return progress.getCompletedQuestsInADay().getCount() == 10;
        }
    }
}
