# Demo Makefile for Hadoop MapReduce and Pig Latin CS6240.

# Customize these paths for your environment.
# -----------------------------------------------------------
# Commonly modified fields:
# Supports Hadoop and Pig (when you want to run Pig Latin, replace Hadoop with Pig)
application.type=Hadoop
# Local Execution:
job.hadoop=neu.cs6240.test_train_split.TrainTestSplitDriver
job.pig=pig-avg-joinFirst-v1.pig
local.input=input_joined
local.output=output_joined

# AWS EMR Execution:
aws.cluster.id=j-224WT8FP9TKSO
aws.num.nodes=7

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
aws.input=input
aws.output=output
aws.log.dir=log
aws.instance.type=m4.xlarge

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
local-hadoop: jar clean-local-output
	${hadoop.root}/bin/hadoop jar ${jar.path} ${job.hadoop} ${local.input} ${local.output}

local-pig: jar clean-local-output
	pig -p INPUT=${local.input} -p OUTPUT=${local.output} src/main/pig/${job.pig}

local:
ifeq (${application.type},Hadoop)
	make local-hadoop
endif
ifeq (${application.type},Pig)
	make local-pig
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


aws-step-hadoop: jar upload-jar delete-output-aws
	aws emr add-steps \
	--cluster-id ${aws.cluster.id}  \
	--steps '[{"Args":["${job.hadoop}","s3://${aws.bucket.name}/${aws.input}","s3://${aws.bucket.name}/${aws.output}"],"Type":"CUSTOM_JAR","Jar":"s3://${aws.bucket.name}/${jar.name}","ActionOnFailure":"CONTINUE","Name":"JAR Application"}]'

aws-step-pig: jar upload-pig delete-output-aws
	aws emr add-steps \
	--cluster-id ${aws.cluster.id}  \
	--steps Type=Pig,Name="Pig Application",ActionOnFailure=CONTINUE,Args=["-f","s3://${aws.bucket.name}/${job.pig}","-p","INPUT=s3://${aws.bucket.name}/${aws.input}","-p","OUTPUT=s3://${aws.bucket.name}/${aws.output}"]


aws-step:
ifeq (${application.type},Hadoop)
	make aws-step-hadoop
endif
ifeq (${application.type},Pig)
	make aws-step-pig
endif

aws-end-cluster:
	aws emr terminate-clusters --cluster-ids ${aws.cluster.id}

aws-pig: jar upload-pig delete-output-aws
	aws emr create-cluster \
		--name "Pig cluster" \
		--log-uri s3://${aws.bucket.name} \
		--release-label ${aws.emr.release} \
		--applications Name=Pig \
		--use-default-roles \
		--instance-groups '[{"InstanceCount":${aws.num.nodes},"InstanceGroupType":"CORE","InstanceType":"${aws.instance.type}"},{"InstanceCount":1,"InstanceGroupType":"MASTER","InstanceType":"${aws.instance.type}"}]' \
		--steps Type=Pig,Name="Pig Application",ActionOnFailure=CONTINUE,Args=["-f","s3://${aws.bucket.name}/${job.pig}","-p","INPUT=s3://${aws.bucket.name}/${aws.input}","-p","OUTPUT=s3://${aws.bucket.name}/${aws.output}"] \
		--enable-debugging \
    --auto-terminate

aws-jar: jar upload-jar delete-output-aws
	aws emr create-cluster \
		--name "WordCount MR Cluster" \
		--release-label ${aws.emr.release} \
		--instance-groups '[{"InstanceCount":${aws.num.nodes},"InstanceGroupType":"CORE","InstanceType":"${aws.instance.type}"},{"InstanceCount":1,"InstanceGroupType":"MASTER","InstanceType":"${aws.instance.type}"}]' \
	    --applications Name=Hadoop \
	    --steps '[{"Args":["${job.hadoop}","s3://${aws.bucket.name}/${aws.input}","s3://${aws.bucket.name}/${aws.output}"],"Type":"CUSTOM_JAR","Jar":"s3://${aws.bucket.name}/${jar.name}","ActionOnFailure":"TERMINATE_CLUSTER","Name":"Custom JAR"}]' \
		--log-uri s3://${aws.bucket.name}/${aws.log.dir} \
		--use-default-roles \
		--enable-debugging \
		--auto-terminate

aws:
ifeq (${application.type},Hadoop)
	make aws-jar
endif
ifeq (${application.type},Pig)
	make aws-pig
endif

# Download output from S3.
download-output-aws: clean-local-output
	mkdir ${local.output}
	aws s3 sync s3://${aws.bucket.name}/${aws.output} ${local.output}

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
