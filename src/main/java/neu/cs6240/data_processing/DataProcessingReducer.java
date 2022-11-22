package neu.cs6240.data_processing;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class DataProcessingReducer extends Reducer<Text, Text, Text, Text> {
    private static String inputvaluesseparator;
    private static String outputseparator;

    @Override
    public void setup(Context context) {
        Configuration configuration = context.getConfiguration();
        inputvaluesseparator = configuration.get("mapreduce.reduce.inputvalues.separator");
        outputseparator = configuration.get("mapreduce.output.textoutputformat.separator");
    }

    @Override
    public void reduce(final Text key, final Iterable<Text> values, final Context context) throws IOException, InterruptedException {
        Map<Integer, Text> h = new HashMap<>();
        Text genre = null;

        for (Text t : values) {
            String[] parts = t.toString().split(inputvaluesseparator);
            String lyricOrGenre = parts[0];

            if (lyricOrGenre.equals("L")) {
                Integer rank = Integer.parseInt(parts[1]);
                String lyricIndex = parts[2];
                h.put(rank, new Text(lyricIndex));
            }
            else if (lyricOrGenre.equals("G")) {
                genre = new Text(parts[1]);
            }
        }

        // only if there is genre and lyric data for a track
        if (genre != null && h.entrySet().size() > 0) {
            StringBuilder sb = new StringBuilder(genre + outputseparator);
            for (int i =0; i<h.entrySet().size(); i++) {
                sb.append(h.get(i));
                if (i<h.entrySet().size() - 1) {
                    sb.append(outputseparator);
                }
            }

            context.write(key, new Text(sb.toString()));
        }
    }
}