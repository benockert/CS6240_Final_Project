package neu.cs6240.knn_prediction;

import neu.cs6240.data_processing.LyricInputMapper;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.filecache.DistributedCache;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Counter;
import org.apache.hadoop.mapreduce.Counters;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.MultipleInputs;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.net.URI;

public class KNNPredictionDriver extends Configured implements Tool {
    private static final Logger logger = LogManager.getLogger(KNNPredictionDriver.class);

    public static enum AccuracyCounters {
        TEST_RECORDS,
        CORRECT_PREDICTION
    }

    public int run(final String[] args) throws Exception {
        final Configuration conf = getConf();
        String[] otherArgs = new GenericOptionsParser(conf, args)
                .getRemainingArgs();
        final Job job = Job.getInstance(conf, "CS6240 Final Project Data Processor");
        job.setJarByClass(KNNPredictionDriver.class);
        final Configuration jobConf = job.getConfiguration();

        job.setMapperClass(KNNPredictionMapper.class);
        job.setReducerClass(KNNPredictionReducer.class);
        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(Text.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);
        FileOutputFormat.setOutputPath(job, new Path(args[2]));

        //Counters
        Counters counters = job.getCounters();
        Counter c1 = counters.findCounter(AccuracyCounters.TEST_RECORDS);
        System.out.println(c1.getDisplayName() + ":  " + c1.getValue());
        Counter c2 = counters.findCounter(AccuracyCounters.CORRECT_PREDICTION);
        System.out.println(c2.getDisplayName() + ":  " + c2.getValue());
        System.out.println("KNN Prediction Accuracy: " + (((double)c2.getValue()) / ((double)c1.getValue())));

        job.addCacheFile(new URI("/prediction_data/text.txt"));

        int res = job.waitForCompletion(true) ? 0 : 1;
        return res;
    }

    public static void main(final String[] args) {
        if (args.length != 3) {
            throw new Error("Three arguments required:\n<lyric-input-dir> <genre-input-dir> <output-dir>");
        }
        try {
            ToolRunner.run(new KNNPredictionDriver(), args);
        } catch (final Exception e) {
            logger.error("", e);
        }
    }
}