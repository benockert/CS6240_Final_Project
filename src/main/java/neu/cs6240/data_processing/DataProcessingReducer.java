package neu.cs6240.data_processing;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

import java.io.IOException;

public class DataProcessingReducer extends Reducer<Text, Text, Text, Text> {
    @Override
    public void reduce(final Text key, final Iterable<Text> values, final Context context) throws IOException, InterruptedException {

    }
}