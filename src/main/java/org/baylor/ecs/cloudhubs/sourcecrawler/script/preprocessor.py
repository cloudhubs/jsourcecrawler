

# @description: tokenize log statement by a separator character
# @param log: single log statement to tokenize
# @param separator: character to tokenize the log statement by
# @return: returns list of strings, each string being a token of the given log line
def tokenize(log, separator):
    return log.split(separator)


# @description: creates a dict of log lines by token length. Maps # of tokens to list of logs
# @param logs: list of log raw log statements
# @param separator: separator character by which to tokenize logs
# @return: dict of log groups, where the key is the number of tokens for each log statement, and the value is
#          the list of tokenized log statements of that group
def form_groups(logs, separator):
    log_groups = {}
    for log in logs:
        token_length = len(tokenize(log, separator))
        if token_length not in log_groups:
            log_groups[token_length] = []
        log_groups[token_length].append(tokenize(log, separator))

    return log_groups
