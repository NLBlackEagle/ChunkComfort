package chunkcomfort.comfort;

import chunkcomfort.chunk.ChunkComfortData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GroupScoreCalculator {

    public static int calculate(ChunkComfortData.GroupData group) {

        int remaining = group.limit;
        int score = 0;

        List<Integer> sortedValues = new ArrayList<>(group.counts.keySet());

        sortedValues.sort(Collections.reverseOrder());

        for (int value : sortedValues) {

            if (remaining <= 0) break;

            int count = group.counts.get(value);

            int take = Math.min(count, remaining);

            score += value * take;

            remaining -= take;
        }

        group.currentScore = score;

        return score;
    }
}