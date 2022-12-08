package neu.cs6240.knn_prediction;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Counter;
import org.apache.hadoop.mapreduce.Counters;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.io.File;

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
        FileInputFormat.addInputPath(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));

        //Counters
        Counters counters = job.getCounters();
        Counter c1 = counters.findCounter(AccuracyCounters.TEST_RECORDS);
        System.out.println(c1.getDisplayName() + ":  " + c1.getValue());
        Counter c2 = counters.findCounter(AccuracyCounters.CORRECT_PREDICTION);
        System.out.println(c2.getDisplayName() + ":  " + c2.getValue());
        System.out.println("KNN Prediction Accuracy: " + (((double)c2.getValue()) / ((double)c1.getValue())));

        File dir = new File("input_test/");
        for(File f : dir.listFiles()) {
            String cache_file_prefix = "input_test/test";
            if(f.toPath().toString().substring(0, cache_file_prefix.length()).equals(cache_file_prefix)) {
                job.addCacheFile(f.toURI());
            }
        }
//        job.addCacheFile(new URI("/input_test/text.txt"));

        return job.waitForCompletion(true) ? 0 : 1;
    }

    public static void main(final String[] args) {
        if (args.length != 2) {
            throw new Error("Two arguments required:\n<input-dir> <output-dir>");
        }
        try {
            ToolRunner.run(new KNNPredictionDriver(), args);
        } catch (final Exception e) {
            logger.error("", e);
        }
    }
}
