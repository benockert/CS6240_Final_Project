# CS6240 Final Project
Aaron Leung, Daniel Jun, Ben Ockert

map 1:
line by line for lyrics data
    if line starts with # or %: do nothing

    # for comma spearated lyrics data
    for all {index:count_of_occurences}:
        determine top 20 indexes

    emit(track_id, top_20_words)

map 2:
line by line for genre data:
    genre = line[1]
    emit(track_id, genre)

reduce: input = track_id, [[top 20], genre]
    
output:
    TRAABRX12903CC4816 pop 19 14 350 600 ...
    ...

    

    
        
        
