from src import preprocessor, similarity, signature_extraction
import sys, json


# @description: group and cluster log statements of a given log file. Then, find the variable elements of the logs.
# @param log_filename: path to the log file to read from and cluster
# param separator: character to tokenize log statements by.
# param threshold_1: percentage value 0 < x < 1 that determines the percentage of tokens in a log statement
#                    that should be equal to corresponding tokens in order to cluster. For example, a value of
#                    0.5 states that 50% of tokens of a log statement should equal the tokens of another to cluster.
# param threshold_2: percentage value 0 < x < 1 that determines the percentage of tokens in a log statement that should
#                    be similar to corresponding tokens in order to merge clusters. For example, a value of 0.5 states
#                    that 50% of tokens in a log statement be similar to another before merging their clusters.
# return: dictionary of log groups that have been fully clusters
#         dict = {key=token_size, value=list of final_cluster}
#         final_cluster = [ list of tokenized log statements, variable_list]
#         variable_list = list of boolean values that indicate whether the token at the same index in the given cluster
#                         is variable or not.
def cluster(log_filename, separator=' ', threshold_1=0.5, threshold_2=0.9):
    # read log statement into lines
    with open(log_filename, "r") as f:
        input_logs = f.readlines()

    # get log groups
    log_groups = preprocessor.form_groups(input_logs, separator)

    # cluster each group
    for key in log_groups.keys():

        # replace log statements with clusters
        log_groups[key] = similarity.cluster_group(log_groups[key], threshold_1)

        # merge clusters
        log_groups[key] = similarity.merge_clusters(log_groups[key], threshold_2)

        # transform clusters into final clusters (find variables)
        log_groups[key] = signature_extraction.create_group_output(log_groups[key])

    return log_groups


if __name__ == "__main__":
    # reserved for later
    if len(sys.argv) < 5:
        print("Incorrect usage: main.py <input file> <output file> <t1> <t2> [<separator>=' '] ")
        sys.exit(1)

    sep = ' '

    if len(sys.argv) == 6:
        sep = sys.argv[5]

    output = cluster(sys.argv[1], sep, float(sys.argv[3]), float(sys.argv[4]))

    with open(sys.argv[2], "w") as output_file:
        json.dump(output, output_file, indent=2)
