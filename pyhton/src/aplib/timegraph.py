#!/usr/local/bin/python3

# A script to plot a graph on (nummeric) values recorded in an csv-input file (comma-separated). 
# One of the column must represent time. Each row in the file is interpreted as (nummeric) data
# sampled at a given time. If the rows have columns named, say, p and q, the script can produce
# a graph showing how the value of p and q develop over time (time-graph). You can select any
# set of properties (column-names) to plot in the output graph. Values of selected properties
# will be plotted on the same graph.
#
#  General syntax to use this script:
#   
#     > timegraph.py [options] arg1 arg2 ...
#
#  The args is a list of property-names whose values you want to include in the resulting graph.
#
#  You can do "timegraph.py --help" to get some help-info. 
#
#  Example usages:
#
#     > timegraph.py -i input.csv health  (if health is a property-name in the csv-file)
#     > timegraph.py -i input.csv --tname t health  (if time is called "t" in the csv-file)
#
#  Options:
#  -i filename   : specify the input csv-file
#  -o output     : specify the name of the file to save the resulting graph (default is tgraph.png)
#  --tname=TNAME : label-name of time in the csv-file (default is "time")

saveToFile = True
import sys
from optparse import OptionParser
import matplotlib
if saveToFile:
   matplotlib.use('Agg')   # to generate png output, must be before importing matplotlib.pyplot
import matplotlib.pyplot as plt
import matplotlib.patches as mpatches
import numpy as np
import math
#import csv
#import os
import pprint
import statistics
import scipy.stats as scistats
from mygraphlib import loadCSV


def mkTimeProgressionGraph(filename:str,
        selectedProperties,
        timeLabel:str="time",
        outputfile:str="tgraph"
        ):
    """ A function to draw a time-graph.
    
    This plots the values of the properties specified in the selectedProperties over time.

    Parameters
    --------------
    filename  : a csv-file containing input data (comma-separated).
    selectedProperties: a list of property-names (column-names) whole values are to be plotted.
    timeLabel : the name used to denote time. Default is "time".
    outputfile: the name of the outputfile.png. Default "tgraph".
    """
    # read the data from the file:
    dataset = loadCSV(filename)

    plt.ylabel('values', fontsize=12)
    plt.xlabel('time', fontsize=12)
    plt.grid(b=True, axis='y')

    # plot the values of
    for propName in selectedProperties:
       plt.plot([ int(r[timeLabel])  for r in dataset ],
                [ float(r[propName]) for r in dataset ],
                  label = propName , )


    plt.rcParams.update({'font.size': 12})
    #fig.suptitle("Emotion time progression")
    #plt.title(f"{propertiesToDisplay} overtime", fontsize=10)
    plt.title("values overtime", fontsize=10)
    plt.legend()
    if saveToFile : plt.savefig(outputfile)
    else : plt.show()

class TimeGraphCommandLine:
    """
    A class implementing command-line interface to call the function mkTimeProgressionGraph().
    """
    def parseCommandLineArgs(self):
        """ A help function to parse the command-line arguments of this script. """

        parser = OptionParser("usage: %prog [options] arg1 arg2 (each arg is a property-name/column-name in the inputfile that is to be plotted)")
        parser.add_option("-i", dest="inputFile",  help="input csv-file (comma separated)")
        parser.add_option("-o", dest="outputFile", default="tgraph", help="output file (png); default is tgraph.png")
        parser.add_option("--tname", dest="tname",  default="time", help="label-name of time in the csv-file; default is \"time\"")
        
        # parse the options and arguments:
        (options,args) = parser.parse_args()
        #print("xxx", options, "yyy", args)
        #print("a ", options.fileIn)
        if(options.inputFile == None):
            print("   Specify an input-file.")
            exit(2)
        if(args == []):
            print("   Specify at least one property-name whose values are to be plotted.")
            exit(2
            )
        return { "scriptOptions": options, "selectedProperties": args }


    def main(self):
        ## Parsing the command-line arguments
        cmd = self.parseCommandLineArgs()
        print('** Input file: ', cmd["scriptOptions"].inputFile)
        print('** Properties to show: ', cmd["selectedProperties"])

        mkTimeProgressionGraph(
            cmd["scriptOptions"].inputFile,
            cmd["selectedProperties"],
            cmd["scriptOptions"].tname,
            cmd["scriptOptions"].outputFile)


if __name__ == "__main__":
   cli = TimeGraphCommandLine()
   cli.main()
   #mkTimeProgressionGraph("samplePXTracefile.csv",["hope","fear"],"t")
