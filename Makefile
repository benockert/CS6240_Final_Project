# Demo Makefile for Hadoop MapReduce and Pig Latin CS6240.

# Customize these paths for your environment.
# -----------------------------------------------------------
# Commonly modified fields:
# Supports Hadoop and Pig (when you want to run Pig Latin, replace Hadoop with Pig)
application.type=Preprocess

# Local Execution:
job.preprocessing=neu.cs6240.knn_pre.KNNPreProcessingDriver
job.knn=neu.cs6240.knn_prediction.KNNPredictionDriver
job.data_processing=neu.cs6240.data_processing.DataProcessingDriver

local.input=input/lyrics
local.output=output
local.input2=input/genres

local.preprocess_input1=input_train
local.preprocess_input2=input_test
local.preprocess_output=output_preprocessing

local.knn_input=input_prediction
local.knn_output=output_knn_final

# AWS EMR Execution:
aws.cluster.id=j-1WP8SSJOB751E
aws.num.nodes=3

# Less frequently modified fields:
# Local Execution:
hadoop.root=$(HADOOP_HOME)
jar.name=cs6240-final-project.jar
jar.path=target/${jar.name}
local.log=log
# AWS EMR Execution:
aws.emr.release=emr-5.17.0
aws.region=us-east-1
aws.bucket.name=junda-cs6240
aws.subnet.id=subnet-6356553a
aws.log.dir=log
aws.instance.type=m5.xlarge

#AWS DataProcessing
aws.input=input/lyrics
aws.input2=input/genres
aws.output=output

#AWS PreProcessing
aws.preprocess_input1=input_train
aws.preprocess_input2=input_test
aws.preprocess_output=output_preprocessing

#AWS KNN Prediction
aws.knn_input=input_prediction
aws.knn_output=output_knn_final

#AWS DataProcessing on Hive
local.hive.input=input
local.hive.script=hive
aws.hive.script=script
aws.hive.bucket_name=bockert-cs6240-hive
aws.hive.input=hive_input
aws.hive.output=hive_output
aws.hive.application_type=Hive
aws.emr.hive.release=emr-5.36.0
aws.emr.hive.applications=Hive
# -----------------------------------------------------------

# Compiles code and builds jar (with dependencies).
jar:
	mvn clean package

# Removes local output directory.
clean-local-output:
	rm -rf ${local.output}*

# Removes local log directory.
clean-local-log:
	rm -rf ${local.log}*

# Runs standalone
# Make sure Hadoop  is set up (in /etc/hadoop files) for standalone operation (not pseudo-cluster).
# https://hadoop.apache.org/docs/current/hadoop-project-dist/hadoop-common/SingleCluster.html#Standalone_Operation
local-data: jar clean-local-output
	${hadoop.root}/bin/hadoop jar ${jar.path} ${job.data_processing} ${local.input} ${local.input2} ${local.output}

local-preprocessing:
	rm -rf ${local.preprocess_output}*
	${hadoop.root}/bin/hadoop jar ${jar.path} ${job.preprocessing} ${local.preprocess_input1} ${local.preprocess_input2} ${local.preprocess_output}

local-knn:
	rm -rf ${local.knn_output}*
	${hadoop.root}/bin/hadoop jar ${jar.path} ${job.knn} ${local.knn_input} ${local.knn_output}

local:
ifeq (${application.type},Data)
	make local-data
endif
ifeq (${application.type},Preprocess)
	make local-preprocessing
endif
ifeq (${application.type},KNN)
	make local-knn
endif


# Create S3 bucket.
make-bucket:
	aws s3 mb s3://${aws.bucket.name}

# Empty and delete S3 bucket.
delete-aws:
	aws s3 mb s3://${aws.bucket.name}
	aws s3 rm s3://${aws.bucket.name} --recursive
	aws s3 rb s3://${aws.bucket.name}

# Upload data to S3 input dir.
upload-input-aws: make-bucket
	aws s3 sync ${local.input} s3://${aws.bucket.name}/${aws.input}
	aws s3 sync ${local.input2} s3://${aws.bucket.name}/${aws.input2}

# Delete S3 output dir.
delete-output-aws:
	aws s3 rm s3://${aws.bucket.name}/ --recursive --exclude "*" --include "${aws.output}*"

# Upload application to S3 bucket.
upload-jar:
	aws s3 cp ${jar.path} s3://${aws.bucket.name}

upload-pig:
	aws s3 cp src/main/pig/${job.pig} s3://${aws.bucket.name}

aws-cluster:
	aws emr create-cluster \
	--name "CS6240 cluster ${application.type}" \
	--log-uri s3://${aws.bucket.name}/log \
	--release-label ${aws.emr.release} \
	--applications Name=${application.type} \
	--instance-groups '[{"InstanceCount":${aws.num.nodes},"InstanceGroupType":"CORE","InstanceType":"${aws.instance.type}"},{"InstanceCount":1,"InstanceGroupType":"MASTER","InstanceType":"${aws.instance.type}"}]' \
	--use-default-roles

aws-step-data: jar upload-jar delete-output-aws
	aws emr add-steps \
	--cluster-id ${aws.cluster.id}  \
	--steps '[{"Args":["${job.data_processing}","s3://${aws.bucket.name}/${aws.input}","s3://${aws.bucket.name}/${aws.input2}","s3://${aws.bucket.name}/${aws.output}"],"Type":"CUSTOM_JAR","Jar":"s3://${aws.bucket.name}/${jar.name}","ActionOnFailure":"CONTINUE","Name":"JAR Application"}]'

aws-step-preprocessing:
	aws s3 rm s3://${aws.bucket.name}/ --recursive --exclude "*" --include "${aws.preprocess_output}*"
	aws emr add-steps \
	--cluster-id ${aws.cluster.id}  \
	--steps '[{"Args":["${job.preprocessing}","s3://${aws.bucket.name}/${aws.preprocess_input1}","s3://${aws.bucket.name}/${aws.preprocess_input2}","s3://${aws.bucket.name}/${aws.preprocess_output}"],"Type":"CUSTOM_JAR","Jar":"s3://${aws.bucket.name}/${jar.name}","ActionOnFailure":"CONTINUE","Name":"JAR Application"}]'

aws-step-knn:
	aws s3 rm s3://${aws.bucket.name}/ --recursive --exclude "*" --include "${aws.knn_output}*"
	aws emr add-steps \
	--cluster-id ${aws.cluster.id}  \
	--steps '[{"Args":["${job.knn}","s3://${aws.bucket.name}/${aws.knn_input}","s3://${aws.bucket.name}/${aws.knn_output}"],"Type":"CUSTOM_JAR","Jar":"s3://${aws.bucket.name}/${jar.name}","ActionOnFailure":"CONTINUE","Name":"JAR Application"}]'

aws-step:
ifeq (${application.type},Data)
	make aws-step-data
endif
ifeq (${application.type},Preprocess)
	make aws-step-preprocessing
endif
ifeq (${application.type},KNN)
	make aws-step-knn
endif

aws-end-cluster:
	aws emr terminate-clusters --cluster-ids ${aws.cluster.id}

# Download output from S3.
download-output-data: clean-local-output
	mkdir ${local.output}
	aws s3 sync s3://${aws.bucket.name}/${aws.output} ${local.output}

download-output-knn: clean-local-output
	mkdir ${local.output}
	aws s3 sync s3://${aws.bucket.name}/${aws.knn_output} ${local.knn_output}

download-output:
ifeq (${application.type},Data)
	make download-output-data
endif
ifeq (${application.type},KNN)
	make download-output-knn
endif

download-log-aws: clean-local-log
	mkdir ${local.log}
	aws s3 sync s3://${aws.bucket.name}/${aws.log.dir}/* ${local.log}

# Change to standalone mode.
switch-standalone:
	cp config/standalone/*.xml ${hadoop.root}/etc/hadoop

# Package for release.
distro:
	rm -f MR-Demo.tar.gz
	rm -f MR-Demo.zip
	rm -rf build
	mkdir -p build/deliv/MR-Demo
	cp -r src build/deliv/MR-Demo
	cp -r config build/deliv/MR-Demo
	cp -r input build/deliv/MR-Demo
	cp pom.xml build/deliv/MR-Demo
	cp Makefile build/deliv/MR-Demo
	cp README.txt build/deliv/MR-Demo
	tar -czf MR-Demo.tar.gz -C build/deliv MR-Demo
	cd build/deliv && zip -rq ../../MR-Demo.zip MR-Demo

# Hive
make-hive-bucket:
	aws s3 mb s3://${aws.hive.bucket_name}

# Upload same data to specific directory for hive to use
upload-hive-input-aws: make-hive-bucket
	aws s3 sync ${local.hive.input} s3://${aws.hive.bucket_name}/${aws.hive.input}

upload-hive-script: make-hive-bucket
	aws s3 sync ${local.hive.script} s3://${aws.hive.bucket_name}/${aws.hive.script}

aws-hive-cluster:
	aws emr create-cluster \
	--name "CS6240 cluster ${aws.hive.application_type}" \
	--log-uri s3://${aws.hive.bucket_name}/hive/logs \
	--release-label ${aws.emr.hive.release} \
	--applications Name=${aws.emr.hive.applications} \
	--instance-groups '[{"InstanceCount":${aws.num.nodes},"InstanceGroupType":"CORE","InstanceType":"${aws.instance.type}"},{"InstanceCount":1,"InstanceGroupType":"MASTER","InstanceType":"${aws.instance.type}"}]' \
	--use-default-roles

# Manually create hive step