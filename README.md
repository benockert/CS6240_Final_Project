# CS6240 Final Project
Aaron Leung, Daniel Jun, Ben Ockert

## KNN Prediction MapReduce Running Instructions
1. Start AWS Lab
2. Use “sudo nano ~/.aws/credentials” to enter session credentials into terminal
3. Update bucket in Makefile. Run “make upload-input-aws”
4. In Makefile, update “aws.num.nodes=“ to number of nodes desired
5. In Makefile, set “application.type=Hadoop”. Run “make aws-cluster”. Copy the clusterID and in Makefile set  aws.cluster.id=<copied_cluster_id>. Wait until cluster is set to state “Waiting”
6. In Makefile, set “application.type=Data”. Run “make local” and wait for job to finish.
    1. EXPECTED TIME: ~1 min   ;    EXPECTED DATA GENERATION: ~61MB
    2. Download log files and note down performance
    3. Go to your S3 bucket. Create 2 new folders: input_test and input_train.
    4. Output of this job should have saved to folder “output/“. Move the test-r-0000x files into the folder input_test. Move the train-r-0000x files into the folder input_train.
    5. (Optional) run “make download-output” to download data from this step
7. In Makefile, set “application.type=Preprocess”. Run “make aws-step” and wait for job to finish.
    1. EXPECTED TIME: ~8 min   ;    EXPECTED DATA GENERATION: ~110GB
    2. Download log files and note down performance
    3. Go to your S3 bucket. Create 1 new folder: input_prediction
    4. Output of this job should save to folder “output_preprocessing/“. Move the result file into folder input_prediction.
    5. NOTE: The output of preprocess should not be downloaded as the data volume will be very large (~110GB)
8. In Makefile, set “application.type=KNN”. Run “make aws-step” and wait for job to finish.
    1. EXPECTED TIME: 60min for 4 machines, 30 minutes for 8 machines   ;    EXPECTED DATA GENERATION: ~30KB
    2. Download log files and note down performance
        1. Stderr log will include prediction accuracy
    3. Run “make download-output” to download output of the final KNNPrediction job
9. CLEANUP
    1. Make sure all logs have been saved to local
    2. Run “make aws-end-cluster”
    3. Run “make delete-aws"
    4. Close lab
    

    
        
        
