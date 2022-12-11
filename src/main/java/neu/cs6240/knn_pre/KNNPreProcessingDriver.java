package neu.cs6240.knn_pre;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.MultipleInputs;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.ArrayList;

public class KNNPreProcessingDriver extends Configured implements Tool {
    private static final Logger logger = LogManager.getLogger(KNNPreProcessingDriver.class);

    public static class TrainInputMapper extends Mapper<LongWritable, Text, NullWritable, Text> {
        @Override
        public void map(final LongWritable offset, final Text lineText, final Context context) throws IOException, InterruptedException {
            String line = lineText.toString();
            context.write(NullWritable.get(), new Text("train-" + line));
        }
    }

    public static class TestInputMapper extends Mapper<LongWritable, Text, NullWritable, Text> {
        @Override
        public void map(final LongWritable offset, final Text lineText, final Context context) throws IOException, InterruptedException {
            String line = lineText.toString();
            context.write(NullWritable.get(), new Text("test-" + line));
        }
    }

    public static class CrossJoinReducer extends Reducer<NullWritable, Text, Text, Text> {
        @Override
        public void reduce(final NullWritable key, final Iterable<Text> values, final Context context) throws IOException, InterruptedException {
            ArrayList<Text> test = new ArrayList<>();
            ArrayList<Text> train = new ArrayList<>();
            String testLabel = "test-";
            String trainLabel = "train-";

            //split records into test and train lists
            for (Text t : values) {
                String textValue = t.toString();
                if(textValue.substring(0, testLabel.length()).equals(testLabel)) {
                    test.add(new Text(textValue.substring(testLabel.length())));
                } else {
                    train.add(new Text(textValue.substring(trainLabel.length())));
                }
            }

            //cross join test and train files
            for(Text testRecord : test) {
                for(Text trainRecord : train) {
                    //write to context
                    context.write(testRecord, trainRecord);
                }
            }
        }
    }

    public int run(final String[] args) throws Exception {
        final Configuration conf = getConf();
        final Job job = Job.getInstance(conf, "CS6240 Final Project Data Processor");
        job.setJarByClass(KNNPreProcessingDriver.class);
        final Configuration jobConf = job.getConfiguration();

        jobConf.set("mapreduce.input_test.testfile.separator", ",");
        jobConf.set("mapreduce.input_train.trainfile.separator", ",");
        jobConf.set("mapreduce.output.textoutputformat.separator", ":");

        MultipleInputs.addInputPath(job, new Path(args[0]), TextInputFormat.class, TrainInputMapper.class);
        MultipleInputs.addInputPath(job, new Path(args[1]), TextInputFormat.class, TestInputMapper.class);

        job.setNumReduceTasks(1);
        job.setReducerClass(CrossJoinReducer.class);
        job.setMapOutputKeyClass(NullWritable.class);
        job.setMapOutputValueClass(Text.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);
        FileOutputFormat.setOutputPath(job, new Path(args[2]));

        int res = job.waitForCompletion(true) ? 0 : 1;
        return res;
    }

    public static void main(final String[] args) {
        if (args.length != 3) {
            throw new Error("Three arguments required:\n<train-input-dir> <test-input-dir> <output-dir>");
        }

        try {
            ToolRunner.run(new KNNPreProcessingDriver(), args);
        } catch (final Exception e) {
            logger.error("", e);
        }
    }
}