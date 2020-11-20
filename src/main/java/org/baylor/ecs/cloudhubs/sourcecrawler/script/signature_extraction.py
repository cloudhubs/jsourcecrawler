from src import preprocessor


# @description: find the variable elements of a given log cluster by going through the columns of each log statement
#               in the cluster and comparing them to the candidate log statement. Initialize all tokens to not a
#               variable, and upon finding a token that does not match the candidate, mark that index as variable.
# @param cluster: log cluster to find the variables for
# @return: list of boolean values, with the value at a given index indicating whether the token at the same index in a
#          log statement is variable.
def identify_variables(cluster):
    # create boolean array, where value j at index i indicates whether or not that column is variable
    variable_list = []
    for i in range(len(cluster[0])): # Fixed the indexing issue
        variable_list.append(False)

    # first item in cluster is candidate
    sample_item = cluster[0]

    print(f'Cluster length is: {len(cluster[0])}')
    print(f'Sample item length is: {len(sample_item)}')

    for log in cluster:

        
        # check each column of the log statement
        for i in range(len(log)):

            # TODO: Odd exception with variable list length different from sample item and log[i]
            print(f'Log {i} length: {len(log)}')
            try:
                if not variable_list[i] and sample_item[i] != log[i]:
                    variable_list[i] = True
            except Exception as e:
                continue
        print()
            
    return variable_list


# @description: given a list of clusters of a given group, convert them to new, final output, which is
#               simple 2 value list [list of clusters, variable indexes]
# @param clusters: list of clusters of a given gorup
# return: list of final clusters. A final cluster is a list with 2 entries:
#         final_cluster[0] = list of tokenized log statements of the cluster
#         final_cluster[0] = list of boolean values, with the value at a given index indicating whether the token
#                            at the same index in a log statement is variable.
def create_group_output(clusters):
    final_clusters = []
    for cluster in clusters:
        new_cluster = [cluster, identify_variables(cluster)]
        final_clusters.append(new_cluster)

    return final_clusters
