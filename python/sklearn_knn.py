# IMPORTS
import pandas as pd
import matplotlib.pyplot as plt
from sklearn.model_selection import train_test_split
from sklearn.neighbors import KNeighborsClassifier
from sklearn.metrics import classification_report
from sklearn import metrics

# CONSTANTS
TEST_SPLIT = 0.2

# DATA INGESTION
# need to untar ../data/data-5000-features.tar.gz and rename file with .csv
song_genre_df = pd.read_csv('../data/python-knn-input.csv', header=None)

# DATA TRANSFORMATION

# Drop the track ID column
song_genre_df = song_genre_df.drop(song_genre_df.columns[0], axis=1)

# Visualization of the occurrences of each genre
genres = song_genre_df[song_genre_df.columns[0]].value_counts()
print(genres)

unique = song_genre_df[song_genre_df.columns[0]].unique()
print(unique)
# genre to integer mapping (better for KNN)
genre_map = {'Pop': 1, 'Jazz': 2, 'Pop_Rock': 3, 'Rock': 4, 'Metal': 5, 'Rap': 6, 
            'Reggae': 7, 'Blues': 8, 'Electronic': 9, 'Punk': 10, 'Country': 11, 
            'RnB': 12, 'Latin': 13, 'Folk': 14, 'New Age': 15, 'World': 16, 
            'International': 17, 'Vocal': 18}

# convert string genres to integer representation i.e. Pop will now be 1, Jazz 2, Vocal 18 etc.
song_genre_df[song_genre_df.columns[0]] = song_genre_df[song_genre_df.columns[0]].map(genre_map)

## BUILDING THE MODEL

# split into predictor features and target feature
df_predictor= song_genre_df.iloc[:, 1:5002]
df_target= song_genre_df.iloc[:, 0:1] # genre column

# split into test and train dataframes
X_train, X_test, y_train, y_test = train_test_split(df_predictor, df_target, test_size=TEST_SPLIT, random_state=0)
print(X_train.shape)
print(X_test.shape)
print(y_train.shape)
print(y_test.shape)

model = KNeighborsClassifier()
model.fit(X_train,y_train)
y_pred = model.predict(X_test)
print("Accuracy: ", metrics.accuracy_score(y_test, y_pred))
print(classification_report(y_test, y_pred))


# SECOND MODEL WITH ADDITIONAL HYPERPARAMETERS
model = KNeighborsClassifier(n_neighbors=7, weights='distance', n_jobs=-1)
model.fit(X_train,y_train)
y_pred = model.predict(X_test)
print("Accuracy: ", metrics.accuracy_score(y_test, y_pred))
print(classification_report(y_test, y_pred))