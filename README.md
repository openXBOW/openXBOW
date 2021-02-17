# 
<img src="openXBOW-text.png" alt="openXBOW" width="600" />

**openXBOW - the Passau Open-Source Crossmodal Bag-of-Words Toolkit**  

openXBOW generates a bag-of-words representation from a sequence of numeric and/or textual features, e.g., acoustic LLDs, visual features, and transcriptions of natural speech. 
The tool provides a multitude of options, e.g., different modes of vector quantisation, codebook generation, term frequency weighting and methods known from natural language processing.  
Below, you find a **tutorial** that helps you to starting working with openXBOW.

The development of this toolkit has been supported by the European Union's Horizon 2020 Programme under grant agreement No. 645094 (IA SEWA) and the European Community's Seventh Framework Programme through the ERC Starting Grant No. 338164 (iHEARu).  

<img src="http://sewaproject.eu/images/sewa-logo.png" alt="SEWA" width="200" /> <img src="http://www.schuller.it/iHEARu-logo.png" alt="iHEARu" width="320" /> ![EU](http://sewaproject.eu/images/eu-logo.png "EU") ![Horizon2020](http://sewaproject.eu/images/horizon_20201.jpg "Horizon2020")  

For more information, please visit the official websites:  
http://sewaproject.eu  
http://ihearu.eu  

(C) 2016-2021, published under GPL v3, please check the file LICENSE.txt for details.
Maximilian Schmitt, Björn Schuller: University of Passau, University of Augsburg.  
Contact: maximilian.schmitt@mailbox.org  


# Citing

If you use openXBOW or any code from openXBOW in your research work, you are kindly asked to acknowledge the use of openXBOW in your publications.  

http://www.jmlr.org/papers/v18/17-113.html  
Maximilian Schmitt and Björn Schuller: "openXBOW - Introducing the Passau Open-Source Crossmodal Bag-of-Words Toolkit", The Journal of Machine Learning Research, Volume 18, No. 96, pp. 1-5, October 2017.  


# Tutorial

In this tutorial, you will learn to start using openXBOW for your reasearch.  
openXBOW is written in Java. You can either generate the jar file on your own or use the precompiled version directly.  
To display a general help text for openXBOW and get a list of all available parameters (including a description), simply run openXBOW in your console:  

    java -jar openXBOW.jar

Make sure that the props folder containing the information about the command line options is in the same folder as the jar file. 
Windows users might need to call Java like this:  

    java.exe -jar openXBOW.jar

In the following, four examples on how to use opemXBOW are given, highlighting several common usecases of BoW processing.  

## Example 1 - Generation of a Bag-of-Words Representation from Numeric Low-Level Descriptors

A typical use case would be the classification of audio segments or images. For each audio/image document, a certain number of feature vectors, i.e., numeric low-level descriptors (LLDs), e.g., MFCCs, SIFT, etc. are given.  
From all LLDs belonging to one document/sample, a bag-of-words representation should be created.  
In the folder `examples/example1`, you find two files `llds.arff` and `llds.csv`, which contain exactly the same information, but differ only in the format (ARFF, used by the machine learning software *Weka*) and CSV. You can always use either of the two formats, depending on your personal preference. The first attribute (or column) is always a string specifying the sample name the LLDs belong to. The file `labels.csv` is a list of lables for each sample. Please note that a CSV (separator `;`) file and a **header** line are required here.  
The file `llds_labels.arff` is another representation of the same LLDs, but here, the labels are directly included in the ARFF file. The labels can also be included in the same way as a column in the CSV file (this is not shown here). However, in case the labels are provided with the LLDs, the value for the target attribute/column must be constant throughout the LLDs of the same document. It is even possible to have multiple labels (targets) for each segment. Then, each target must have its own attribute (ARFF) or column (CSV).  

Now, let's start to generate a bag-of-words output for the sample data. The three following lines using different input formats should all create the same output arff bag-of-words file.

    java -jar openXBOW.jar -i examples/example1/llds.arff -l examples/example1/labels.csv -o bow.arff
    java -jar openXBOW.jar -i examples/example1/llds.csv  -l examples/example1/labels.csv -o bow.arff
    java -jar openXBOW.jar -i examples/example1/llds_labels.arff -o bow.arff
  
The command line option `-i` specifies the input file and `-o` the output file. The file format is always recognised automatically based on the file ending. Option `-l` specifies the label file.  
Of course it is always possible to generate bag-of-words output without a target (class label), such as 

    java -jar openXBOW.jar -i examples/example1/llds.csv -o bow.arff

A CSV output is generated using `-o bow.csv`. Also LibSVM (Liblinear) output is supported (`-o bow.libsvm`):

    java -jar openXBOW.jar -i examples/example1/llds.csv -o bow.csv
    java -jar openXBOW.jar -i examples/example1/llds.csv -o bow.libsvm

Note that, in case of a libsvm output, the labels must be *integers*.  

As you can see from the examples, the default **codebook size** is *500*. The codebook size (in case of numeric input) can be modified using the parameter `-size`.  
Also the **number of assignments**, i.e., the number of words in the bag representation, where the counter is increased for each input LLD, can be modified. The default number of assigments is *1*, for *multi assignment*, use the option `-a`. As distance measure, the Euclidean distance is always used.  

To generate a bag-of-words representation with a codebook size of 1000, and 10 assignments per input LLD, use the following command line:

    java -jar openXBOW.jar -i examples/example1/llds_labels.arff -o bow.arff -size 1000 -a 10

Until now, we generated the codebook by a **random sampling** of all LLDs in the input file. More precisely, per default, the initialisation step of the *k-means++* clustering algorithm is executed, but the cluster centroids are not updated. In this initialisation step, the codebook vectors (words/templates) are selected subsequently while favouring vectors which are farther away (in terms of Euclidean distance) from the already selected ones. Random sampling is much faster than clustering while obtaining almost the same final performance. **k-means++** is employed when using `-c kmeans++`. Also the *standard k-means clustering* (`-c kmeans`) and a standard random sampling (`-c random`) are available.  
Right now, we know how to generate different codebooks, but so far, the codebook is always learnt from the whole given input sequence. In supervised learning (as the main application of the bag-of-words representation), however, we usually need to evaluate a method on completely unseen *test data*.  
To accomplish this, the codebook needs to be *stored* and then *loaded* when used on the test data. You can trigger those options using `-B` (store) and `-b` (load).  

In the following line, a codebook of size 200 is learnt using kmeans++ clustering, first and is then stored in the file `codebook`. At the same time, a bag-of-words representation using multi assignment of the input is generated and stored (`bow.arff`).  

    java -jar openXBOW.jar -i examples/example1/llds_labels.arff -o bow.arff -a 5 -c kmeans++ -size 200 -B codebook

In the following line, the learnt codebook is loaded and applied to the same input data. (Applying the codebook to the same data does not make sense, of course, this is just to exemplify the usage.) The format of the codebook is a text format and readable quite easily, you can have a look opening `codebook` with a text editor.

    java -jar openXBOW.jar -i examples/example1/llds_labels.arff -o bow.arff -a 5 -b codebook

Please note that, in case consistency between training and test data is targeted, the parameter for multi assignment `-a 5` must be repeated when processing the test data.  

After the generation of the bag-of-words representation, they can be further processed with the following options:

* Logarithmic term-frequency weighting: `-log` applies `lg(TF+1)` to each term frequency (TF) in the bag. This option is **stored** in the codebook file and used when the respective codebook is loaded.  
* Inverse document frequency (IDF) transform: `-idf`  Each term frequency is multiplied with the logarithm of the ratio of the total number of instances and the number of instances where the respective term/word is present. This option and the corresponding parameters (IDF weights) are stored in the codebook file and used when the respective codebook is loaded.  
* Histogram normalisation: `-norm 1`, `-norm 2`, or `-norm 3`. Please see the help text for information about the differences between the options.  

The histogram normalization is a very common option and especially required when the amount of input LLDs differs between the different input documents/samples.  

Usually, it is also required to **standardise** (or **normalise**) the input LLDs as their values are in different ranges. This can be tackled using the options `-standardizeInput` or `-normalizeInput`. Note that, the corresponding parameters derived from the data (in case of standardisation: mean and std. deviation) are also *stored* in the codebook and are then applied to test data (*online* approach). The same goes for the standardisation/normalisation of the resulting bag-of-words representation. This is done by openXBOW using `-standardizeOutput` or `-normalizeOutput`. Standardisation/normalisation of the features is required (or at least useful) in some machine learning algorithms, such as, e.g., support vector machine.  

Further options can be seen in the default help text on the command line.  


## Example 2 - Generation of a time-dependent Bag-of-Words Representation from time-dependent Numeric Low-Level Descriptors

A possible scenario for the data discussed in this example is the time-dependent (discrete time-continuous) prediction of speaker traits (e.g., emotion) from audio.  
In this case, the bags are created over a certain *segment* or *block* in time.

This can be triggered using the option `-t` with the two parameters *block/window size* and *hop size* (both in seconds). Using this option requires that the seconds attribute (or column in case of CSV) is a time stamp of the corresponding LLD vector.

Use the following lines to generate time-dependent bags from the data of example2:

    java -jar openXBOW.jar -i examples/example2/llds.csv -l examples/example2/labels.csv -t 5.0 0.5 -o bow.arff -a 1 -c kmeans++ -size 200 -B codebook
    java -jar openXBOW.jar -i examples/example2/llds.csv -l examples/example2/labels.csv -t 5.0 0.5 -o bow.arff -a 1 -b codebook

The name of the sequences and the time stamps of the resulting frames can be added in the output files like this:

    java -jar openXBOW.jar -i examples/example2/llds.csv -l examples/example2/labels.csv -t 5.0 0.5 -o bow.arff -a 1 -c kmeans++ -size 200 -B codebook -writeName -writeTimeStamp

Here, a block size of 5.0 seconds is taken into account to generate the bags, i.e., a period of 2.5 seconds around each time stamp specified by the hop size 0.5 seconds. Note that the hop size in the given labels file (2nd column) must correspond to that specified in the command line.

It is also possible to further split the input llds into two or more subsets, where for each subset, a separate codebook is trained. The bags are then generated independently for each subset of LLDs and finally fused. This can be configured with the `-attributes` options which is followed by a string specifying the function of each input column (or attribute in ARFF). More information is found in the help text. Here is an example, where the first 5 LLDs form the first subset (with codebook 1) and the second 5 LLDs form the second subset (with codebook 2). Note that the size of each codebook is also specified by the `size` option. The first attribute specifies the name (`n`), the second the time stamp (`t`). After the codebook indexes (`1`) and (`2`), the number of subsequent LLDs in the corresponding subset are specified in brackets `[]`.

    java -jar openXBOW.jar -i examples/example2/llds.csv -attributes nt1[5]2[5] -l examples/example2/labels.csv -t 5.0 0.5 -o bow.arff -a 1 -c kmeans++ -size 50,100 -B codebook -writeName -writeTimeStamp


## Example 3 - Generation of a Bag-of-Words Representation from Crossmodal Input Data

In this example, we look at crossmodal (multimodal - numeric and symbolic) input.  
In the folder `example3`, you find a simple example for sentiment analysis (positive,negative) based on crossmodal input data. The first attribute in this ARFF file specifies the name (as always required) of each sample to be classified. Then, there are four numeric descriptors (e.g., acoustic and/or visual features), a string attribute with the transcription of what is said and the class label (pos,neg). Please note that the data here are random and this is just to exemplify the format required for openXBOW. E.g., in a real setting using acoustic features and text, the rate of the acoustic features would be much higher than the rate of the words. Thus, much more text would be empty `''`. All input having the same `name` attribute is now transformed into a bag-of-words representation:

    java -jar openXBOW.jar -i examples/example3/crossmodal.arff -o bow.arff -c kmeans++ -size 4 -B codebook -writeName

If you open the generated `codebook` with a text editor, you will see that two codebooks have been generated automatically, i.e., a symbolic codebook (`codebookText` or dictionary) and a numeric codebook (`codebookNumeric`). The first four lines after the `codebookText` statement specify characters that are always removed from the text input, the number of *n-grams* and *n-character-grams* (default is 1-grams and no character-grams) and the number of words.  
Note that all text is converted to uppercase automatically as in a crossmodal use case, the text input usually originates from an automatic speech recognition system which outputs mostly all in uppercase. Furthermore, the case implies usually very little information.  
The bags from the two (or more, as we have not explicitly specified the numeric LLDs in this example) domains are then concatenated in an **early fusion** approach to form the output file `bow.arff`.

If you prefer to use **late fusion**, this is also possible of course with openXBOW. Use the two following lines to generate in-domain bag-of-words:

    java -jar openXBOW.jar -i examples/example3/crossmodal.arff -attributes n1[4]rc -o bowNumeric.arff -c kmeans++ -size 4 -B codebookNumeric -writeName
    java -jar openXBOW.jar -i examples/example3/crossmodal.arff -attributes nr[4]0c -o bowText.arff -B codebookText -writeName

The `attributes` option is essential here as it removes the input features not wanted `r`. **The index `0` is always reserved for the symbolic (text) input**, so, the 6th attribute in the input file is text, the last attribute (`c`) is the class label.

So far, no further options for natural language processing have been used. Go on with the next tutorial to learn more about those.


## Example 4 - Sentiment analysis in tweets (text-only) using Bag-of-Words

Now, we want to perform sentiment analysis on tweets (Twitter). For that purpose, you need to download the Twitter Sentiment Analysis Dataset from the following website:

http://thinknook.com/twitter-sentiment-analysis-training-corpus-dataset-2012-09-22  
Here is a direct download link: http://thinknook.com/wp-content/uploads/2012/09/Sentiment-Analysis-Dataset.zip  

The zip-archive contains a CSV file with 1,578,627 annotated tweets. As you see, the first column is the ID (name, `n`), the second column the sentiment (0 or 1 = target = `c`), the third column the *source* of the data which is not required here and therefore *removed* (`r`), and the last column is the text (codebook `0`). Given the huge amount of data and the almost infinite number of words, especially usernames (@xqz) etc., you need to directly limit the dictionary size. This is done by setting a minimum term frequency, i.e., the minimum number of occurrences of a word to be incorporated into the dictionary. As there are more than 1,000,000 tweets, a minimum term frequency of `2000` could be a good starting point (`-minTermFreq`). There is also the option to set a maximum term frequency (`-maxTermFreq`) to exclude very common words (such as, e.g., 'and', 'do', 'it', etc.).  
This time, we take into account that we would like to do a proper evaluation of our algorithm. We manually split the CSV file into a training (the first 1,000,000 lines/tweets) and a test partition (the remaining 578,627 lines/tweets) and store each file separately.

    java -Xmx12000m -jar openXBOW.jar -i "Sentiment Analysis Dataset - train.csv" -attributes ncr0 -o bowTwitter-train.arff -minTermFreq 2000 -B dictionaryTwitter
    java -Xmx12000m -jar openXBOW.jar -i "Sentiment Analysis Dataset - test.csv" -attributes ncr0 -o bowTwitter-test.arff -b dictionaryTwitter

Note that you might need to increase your *Java heap space* (`-Xmx12000m`). The option `-minTermFreq` is only relevant for learning the dictionary and is not repeated in the call for the test partition.  
When openXBOW is finished, you can train a classifier on `train` and evaluate it on test (e.g., support vector machine). In case you prefer Liblinear (LibSVM) to Weka, just output a file in LibSVM format (`-o bowTwitter-train.libsvm`).

To use 2-grams (sequences of two words in addition to single words), use the following command lines:

    java -Xmx12000m -jar openXBOW.jar -i "Sentiment Analysis Dataset - train.csv" -attributes ncr0 -o bowTwitter-train.arff -nGram 2 -minTermFreq 2000 -B dictionaryTwitter
    java -Xmx12000m -jar openXBOW.jar -i "Sentiment Analysis Dataset - test.csv" -attributes ncr0 -o bowTwitter-test.arff -nGram 2 -b dictionaryTwitter

Note that, the option `-nGram 2` needs to be repeated also when the learnt codebook is applied to the test partition.  

Now you can play with different configurations and classifiers. Try also the options `-log` and `-idf`. Those two parameters are stored in the codebook file and do not need to be repeated in the call for the test partition.

Using the first 1,000,000 tweets for training and the remaining instances for evaluation, an (unweighted) accuracy of more than 75 % can be obtained.  


## Example 5 - Applying openXBOW to audio classification tasks

Finally, we show examples with a complete processing chain, where openXBOW is used. First, we give here an example for audio classification.
In the next example, a tutorial on image classification is given.

In this example, we want to learn a Bag-of-Audio-Words model for emotion recognition in speech, based on the Berlin Database of Emotional Speech (**EmoDB**).

Before we start, you need to make the following preparations:
* Download **EmoDB** http://www.emodb.bilderbar.info/download/ (complete database) and extract the zip archive into a folder (called `./examples/example5/emodb` in the following).
* Install the sound processing toolkit **sox** http://sox.sourceforge.net/
* Download the **openSMILE** acoustic feature extraction toolkit http://audeering.com/technology/opensmile/ in its latest version (2.3) and extract it to a directory (called `/home/user/opensmile-2.3.0` in the following).
* Install **Python2**, which we will use for scripting https://www.python.org/downloads/
* Install the machine learning toolkit **Weka** http://www.cs.waikato.ac.nz/ml/weka/
Please find the installation instructions on the corresponding websites.

As a first step, we need to extract acoustic low-level descriptors (LLDs). We use openSMILE to extract the LLDs of the **ComParE** (Computational Paralinguistics ChallengE) feature set (info: http://compare.openaudio.eu/).
As openSMILE supports does not support the 32-bit wave files from EmoDB, they are converted to 16-bit files using SoX beforehand.
Moreover, we require a speaker-independent training and test partition in order to obtain a reliable estimate of the performance for unknown speakers.
We use speakers with the IDs 03, 08, 09, and 10 (encoded by the first two characters of the filenames) for the test partition and the remaining 6 speakers (11, 12, 13, 14, 15, and 16) for the training partition.
Finally, we also need a text file with the label for each instance (wave file). The corresponding emotion (A=fear, E=disgust, F=happiness, L=boredom, N=neutral, T=sadness, W=anger) is encoded in the 6th character of the filenames.
All steps to prepare the EmoDB data for feature extraction are abstracted in the Python script `prepare_emodb.py`.

The feature extraction can be done using the script `extract_acoustic_features.py` which can be used for arbitrary audio data in wave (PCM, 16 bits) format.
The ComParE feature set consists of 130 LLDs in total. Information about the corresponding features are given in the header line of the output files.

After executing the two commands

    python prepare_emodb.py
    python extract_acoustic_features.py

openXBOW can be applied.

We introduce the following two variables to keep the arguments of openXBOW simple. They comprise the paths to the input file (audio_llds_train.csv), output file (xbow_train.arff), labels file (labels_train.csv), and codebook file (codebook) given that they are located (to be stored) in the examples/example5/ subfolder of the openXBOW base directory.
Note that, the command for the codebook file differs for the training and the test partition (`-B` vs `-b`).

    IOTRAIN="-i examples/example5/audio_llds_train.csv -o examples/example5/xbow_train.arff -l examples/example5/labels_train.csv -B examples/example5/codebook" 
    IOTEST="-i examples/example5/audio_llds_test.csv -o examples/example5/xbow_test.arff -l examples/example5/labels_test.csv -b examples/example5/codebook"

To launch openXBOW, run the following command on the console:

    java -jar openXBOW.jar $IOTRAIN 

For the resulting BoAW output feature file (xbow_train.arff), we want to train and evaluate a Support Vector Machine classifier in Weka. You can either use the GUI to load the ARFF file, select the classifier *SMO* in the group *functions* and 10-fold cross validation (CV) for evaluation, or you can run Weka on the console (with the corresponding classpath, the given command line assumes that you use Windows and Weka 3.8 is installed in the Program Files directory):

    java -classpath "C:\Program Files\Weka-3-8\weka.jar" weka.classifiers.functions.SMO -t examples/example5/xbow_train.arff

Note that, this kind of evaluation is not speaker-independent (the same speaker may appear in different folds), though speaker-independence is a basic requirement when dealing with speech data. Due to the limited size of the dataset, however, we employ 10-fold CV for a first quick performance estimate. In a later step, we will apply the optimised model on the speaker-independent test partition, in order to obtain a reliable result for the accuracy.

You should see that the obtained accuracy (Weighted Average Recall) is approx. 34 %, which is not a good result for this dataset. Thus, you will need to optimise some parameters in openXBOW.
First of all, we are going to employ standardisation of the input features (LLDs). This step should be beneficial as the LLDs comprise very different ranges of numeric values, which has a huge influence on the word assignment step.

Run the following command on the console:

    java -jar openXBOW.jar $IOTRAIN -standardizeInput

and use the same command for Weka as before.
You will realise that the accuracy has improved tremendously to approx. 72 %.
Further optimisation might include a logarithmic term-frequency weighting, an increased codebook size (default is 500), another number of assignments (default is 1) and a split of the input feature space into different codebooks. The latter is done by defining a certain number of codebooks with a certain number of input features with the `-attributes` option.
Run the following commands successively and Weka after each run of openXBOW. You will recognise that the accuracy improves after each step.

    java -jar openXBOW.jar $IOTRAIN -standardizeInput -log
    
    java -jar openXBOW.jar $IOTRAIN -standardizeInput -log -size 1000
    
    java -jar openXBOW.jar $IOTRAIN -standardizeInput -log -size 1000 -a 5 -attributes nt1[65]2[65]

In the last command, `nt1[65]2[65]` specifies that the 1st attribute (column) in the input file (audio_llds_train.csv) is the `name` (`n`) of the instance, the LLD belongs to. The 2nd attribute, `t`, is the `time stamp`. The following 65 numeric descriptors are concatenated and used as input for the first codebook (`1[65]`). The remaining 65 LLDs are the input for the second codebook (`2[65]`). Split codebooks can indeed contribute to a higher accuracy. The resulting BoW are fused before the post-processing (early fusion).
Note that, `-size 1000` specifies the size of each codebook, i.e., the final size of the BoW vectors is 2000 in the given example. You can also determine the size for each codebook directly, by separating the sizes for each codebook with a comma (`,`): e.g., `-size 1000,500`, to employ a smaller codebook for the LLDs with indexes 66 to 130.

Finally, we end up in an accuracy of approx. 85.9 %. However, this evaluation is not speaker-independent.
Thus, we apply openXBOW to the test partition using the command:

    java -jar openXBOW.jar $IOTEST -attributes nt1[65]2[65]

Note that, the codebook is not learnt here, but the codebook learnt from the training partition is reused. This is done using the option `-b` (with a lowercase `b`) in the $IOTEST. If another codebook was learnt from the test feature set, the resulting BoW would not be compatible at all with the BoW from the training set and so, the trained classifier could not be used.
Furthermore, note that, the `-attributes` argument needs to be repeated for the test data. All other options are stored in the codebook file and are also loaded with the option `-b`.

    java -classpath "C:\Program Files\Weka-3-8\weka.jar" weka.classifiers.functions.SMO -t examples/example5/xbow_test.arff
    
You will see that, an accuracy of approximately 81.4 % can be reached on the speaker-independent test partition.


## Example 6 - Applying openXBOW to image classification tasks

In this tutorial, we exemplify how to apply openXBOW to a simple image classification task, using the Bag-of-Visual-Words (BoVW) approach. As a sample dataset, we use the **Caltech101** dataset.
We are going to train a model to discriminate between 5 different image classes and evaluate using 10-fold cross validation.

Before we start, you need to make the following preparations:
* Download the dataset 101_ObjectCategories.tar.gz from the website: http://www.vision.caltech.edu/Image_Datasets/Caltech101/ and extract it into a folder (called `./examples/example6/caltech101` in the following).
* Install **Python2**, which will be used for scripting https://www.python.org/downloads/
* Install the Python image processing package Scikit-Image http://scikit-image.org/
* Install the machine learning toolkit **Weka** http://www.cs.waikato.ac.nz/ml/weka/
Please find the installation instructions on the corresponding websites.

We consider only a subset of the Caltech101 dataset, namely, the images of the classes *anchor*, *barrel*, *elephant*, *pizza*, and *saxophone*. As a first step, the files in the corresponding folders are copied to the folder `images' and renamed, as the original filenames are not unique over different classes. This is done by running the Python script *prepare_caltech101.py*:

    python prepare_caltech101.py

Next, we need to define, which features we are going to consider. BoVW requires a certain amount of local image descriptors for each image. These can be, e.g., Scale-Invariant Feature Transform (SIFT) features.
In this method, certain keypoints are detected in each image as a first step. Then, local descriptors are generated describing the neighbourhood around these keypoints. In this tutorial, however, we are going to employ the so-called **Daisy** descriptors introduced by Tola et al. in 2010, which are extracted in dense regions all over the image. Calling the Python script *extract_visual_features.py* you are extracting Daisy features for all images and storing them in the CSV file *visual_features.csv*.

    python extract_visual_features.py
    
As in the preceding tutorial, we are going to define a variable for the openXBOW input and output files:

    IOXBOW="-i examples/example6/visual_features.csv -o examples/example6/xbow.arff -l examples/example6/labels.csv -B examples/example6/codebook" 

To launch openXBOW, run the following command on the console:

    java -jar openXBOW.jar $IOXBOW 

For the resulting BoVW output feature file (xbow.arff), we train and evaluate a Support Vector Machine classifier in Weka. You can either use the GUI to load the ARFF file, select the classifier *SMO* in the group *functions* and 10-fold cross validation (CV) for evaluation, or you can run Weka on the console (with the corresponding classpath, the given command line assumes that you use Windows and Weka 3.8 is installed in the Program Files directory):

    java -classpath "C:\Program Files\Weka-3-8\weka.jar" weka.classifiers.functions.SMO -t examples/example6/xbow.arff

You should see that the obtained accuracy (Weighted Average Recall) is approx. 68.7 %. Next, parameters of openXBOW are optimised.
As the standardisation of the input will not have a huge influence when using binary features, we are going to employ logarithmic term-frequency weighting, first of all.
Run the following command on the console:

    java -jar openXBOW.jar $IOTRAIN -log

and use the same command for Weka as before. You will recognise that the accuracy has improved to approx. 69.5 %.
Now, we are going to increase the codebook size and the number of assignments. As you might have noticed, the feature vectors are larger compared to the LLDs used in the tutorial on speech classification.

    java -jar openXBOW.jar $IOXBOW -log -size 4000 -a 3

With a codebook size of 5000 and 5 assignments per LLD, an accuracy of 82.1 % is achieved.
Note that, the same codebook learnt from the whole data set is used for all folds generated in the cross validation, i.e., the expected accuracy on unknown data can be lower than the one obtained.
Reusing a codebook trained on another data set can be done using the openXBOW option `-b' as introduced before (e.g., in example 5).
Further improvement of the accuracy can be obtained by optimising the parameters of the image descriptors (Daisy).


**Thank you for your interest and reading this tutorial!**  
In case you have any questions, be kindly invited to contact Maximilian Schmitt, University of Passau: maximilian.schmitt@uni-passau.de

