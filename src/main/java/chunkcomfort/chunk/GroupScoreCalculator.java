package chunkcomfort.chunk;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Calculates the comfort score of a group while respecting its limit.
 */
public class GroupScoreCalculator {

    /**
     * Calculates the total comfort score for a group, capping at group.limit.
     *
     * @param group The group data
     * @return The calculated score
     */
    public static int calculate(ChunkComfortData.GroupData group) {

        if (group == null || group.counts.isEmpty() || group.limit <= 0) {
            group.currentScore = 0;
            return 0;
        }

        int remaining = group.limit;
        int score = 0;

        // Sort block values descending so higher value blocks count first
        List<Integer> sortedValues = new ArrayList<>(group.counts.keySet());
        sortedValues.sort(Collections.reverseOrder());

        for (int value : sortedValues) {

            if (remaining <= 0) break;

            int count = group.counts.getOrDefault(value, 0);

            // Take as many as possible but never exceed remaining limit
            int take = Math.min(count, remaining);

            score += value * take;
            remaining -= take;
        }

        group.currentScore = score;
        return score;
    }
}