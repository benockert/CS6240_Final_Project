package neu.cs6240.data_processing;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

import java.io.IOException;

public class GenreInputMapper extends Mapper<LongWritable, Text, Text, Text> {
    private final static String genreOutputKey = "G-";
    private static String separator;

    @Override
    public void setup(Context context) {
        Configuration configuration = context.getConfiguration();
        separator = configuration.get("mapreduce.input.genresfile.separator");
    }

    @Override
    public void map(final LongWritable offset, final Text lineText, final Context context) throws IOException, InterruptedException {
        String line = lineText.toString();
        if (!line.startsWith("#")) {
            String[] lineData = line.split(separator);

            String trackId = lineData[0];
            String topGenre = genreOutputKey + lineData[1];

            context.write(new Text(trackId), new Text(topGenre));
        }
    }
}