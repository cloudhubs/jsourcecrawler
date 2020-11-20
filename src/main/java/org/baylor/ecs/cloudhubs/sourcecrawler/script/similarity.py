from difflib import SequenceMatcher
import textdistance
import itertools


# @description: given a list of clusters, attempt to merge them together into a single cluster if candidate tokenized
#            log statements are similar enough.
#            input --> output === list of list of tokenized log statements --> list of list of tokenized log statements.
# @param clusters: list of clusters in a given group.
# @param threshold: threshold percentage 0 < x< 1 that determines the percentage that two log statements are similar.
#                   for example, a threshold of 0.5 means that the total average similarity of all the tokens in the
#                   same column should be 50% or greater in order to merge clusters
# @return: list of clusters [list of list of log statements]
def merge_clusters(clusters, threshold):
    for cluster_1 in clusters:
        for cluster_2 in clusters:
            if cluster_1 is not cluster_2:
                if log_cluster_comparison(cluster_1[0], cluster_2[0]) > threshold:
                    clusters.remove(cluster_1)
                    clusters.remove(cluster_2)
                    cluster_1.extend(cluster_2)
                    clusters.append(cluster_1)

    return clusters


# @description: form clusters of log statements in a given group (group given by length of tokenized log). Compare new
#               log statements with candidates from previous clusters.
#               if logs are equal enough (given by the threshold) then cluster them. Otherwise, create new cluster
#               for new log statement.
#               input --> output === list of tokenized log statements --> list of list of tokenized log statements
# @param log_list: list of tokenized log statements from a given group.
# @param threshold: threshold percentage 0 < x < 1 that determines if two log statements are equal enough to cluster
#                   for example, a threshold of .60 means that 60% of the tokens in both log statements have to be
#                   equal. Tokens are only compared in the same column.
# @return: list of clusters. Format of a cluster is a list of log_statements
def cluster_group(log_list, threshold):
    clusters = []

    for log in log_list:
        if len(clusters) == 0:
            clusters.append([log])
        else:
            clustered = False
            for i in range(len(clusters)):
                if not clustered and log_template_comparison(clusters[i][0], log) > threshold:
                    clusters[i].append(log)
                    clustered = True
            if not clustered:
                clusters.append([log])

    return clusters


# @description: on second pass, uses two candidate log statements from a cluster to determine if the clusters
#               should be matched. Uses tokenized form of the log statements, and compares the similarity of their
#               values. Log statements are assumed to be the same length.
# @param log_a: the first tokenized log statement
# @param log_b: the second tokenized log statement
# @return: the average of the similarities of all the tokens in the log statements
def log_cluster_comparison(log_a, log_b):
    sum = 0
    for i, j in zip(log_a, log_b):
        sum += log_similarity(i, j)

    return sum / len(log_a)


# @description: returns the cosine similarity of two log tokens
# @param i: the first log token
# @param j: the second log token
# @return: the cosine similarity between them
def log_similarity(i, j):
    # return textdistance.Levenshtein.similarity(i, j)
    return textdistance.cosine.similarity(i, j)  # use cosine distance; alternative is Levenshtein distance


# @description: on the first pass, calculates the similarity between two log statements. Assumes both log statements
#               have an equal number of tokens
# @param log_a: the first tokenized log statement
# @param log_b: the second tokenized log statement
# @return: the average similarity between the given tokens
def log_template_comparison(log_a, log_b):
    sum = 0
    for i, j in zip(log_a, log_b):
        sum += token_match(i, j)

    return sum / len(log_a)


# @description: used in first pass matching, determines if two tokens are the same or not
# @param a: the first token
# @param b: the second token
# @return: True if they are the same, False if not
def token_match(a, b):
    return a == b
