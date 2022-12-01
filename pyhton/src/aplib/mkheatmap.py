#!/usr/local/bin/python3

# A script to plot a graph on values recorded in an csv-input file. The file
# has the following format:
#
#    Header: posx,posy,posz,time,prop-name-1,prop-name-2,...
#    The rows follow accordingly.
#    Note: (posx,posy,posx) represents a 3D coordinate of a position/location.
#           posy is for now ignored.
#
# Two type of graphs can be produced:
#
#     (1) time-graph plots the values of chosen properties vs time
#     (2) heatmap shows the values of chosen properties vs position (x,z)
#
#  General syntax to use this script:
#   
#     > mkgraph [options] arg1 arg2 ...
#
#  You can do "mkgraph --help" to get some help-info. 
#  Example usages:
#
#     > mkgraph -i input.csv prop-name-1
#     > mkgraph -i input.csv health  (if health is a property-name in the csv-file)
#
#  You can also do: 
#
#     > mkgraph --help
#
#
#  Options:
#  -i filename   : specify the input csv-file
#  -o output     : specify the name of the file to save the resulting graph (.png format)
#  -g graphtype  : either "timegraph" or "heatmap"
#
#  For heatmaps, we have further options:
#  --hmWidth=w and --hmHeight=h  : the width and height of the produced map (1 unit length
#                                  corespond to one 1-unit length of the coordinate-system
#                                  used to express positions in the input csv-file).
#  --hmScale=s  : you can imagine the heatmap to be constructed from tiles of sxs in size.
#                 Positions in the same tile are then considered as representing the same
#                 position.
#  --hmMinval=a, --hmMaxval=b : the assumed minimum and maximum values of attributes whose values
#                               are to be plotted into the heatmap./
# 

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
import os
import pprint
import statistics
import scipy.stats as scistats
from mygraphlib import loadCSV

# Parsed command-line options and arguments to this script will be set here.
#
#     Available options:
#        scriptOptions.inputFile
#        scriptOptions.graphType
#        scriptOptions.heatmapWidth
#        scriptOptions.heatmapHeight
#        scriptOptions.heatmapValMin
#        scriptOptions.heatmapValMax
#        scriptOptions.heatmapSizeScale
#
#    The script-args specify the properties  from the input data that are
#    to be plotted.

scriptOptions = None
selectedProperties = None


def parseCommandLineArgs():
    """ A help function to parse the command-line arguments of this script. """

    parser = OptionParser("usage: %prog [options] arg1 arg2 (each arg is a property-name/column-name in the inputfile)")
    parser.add_option("-i", dest="inputFile",  help="input csv-file")
    parser.add_option("-o", dest="outputFile", default="plot.png", help="output file")
    parser.add_option("-g", dest="graphType", default="timegraph",
                            choices=["timegraph","heatmap","coldmap"],
                            help="the type of graph")
    parser.add_option("--hmWidth",  dest="heatmapWidth" , help="the width of heatmap")
    parser.add_option("--hmHeight", dest="heatmapHeight", help="the height of heatmap")
    parser.add_option("--hmScale",  dest="heatmapSizeScale", default="1", help="the size-scale of heatmap")
    parser.add_option("--hmMinval", dest="heatmapValMin", default="0",   help="minimum heat value in heatmap")
    parser.add_option("--hmMaxval", dest="heatmapValMax", default="100", help="maximum heat value in heatmap")

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
    global scriptOptions
    global selectedProperties
    scriptOptions = options
    selectedProperties = args



def mkHeatMapWorker(dataset,
        selectedProperties,
        xLabel,
        yLabel,
        countFunction,
        width,
        height,
        scale,
        minvalue,
        maxvalue):
    """ A function to draw heatmap. 
    
    """

    #dataset = loadCSV(filename)
    white = maxvalue
    H = math.ceil(scale*height)
    W = math.ceil(scale*width)
    map = np.zeros((H,W))
    for y in range(0,H):
      for x in range(0,W):
          map[y][x] = white

    for r in dataset:
        x = round(scale*float(r[xLabel]))
        y = round(scale*float(r[yLabel]))

        # combine the value of the properties by summing them:
        value = -minvalue
        for propName in selectedProperties:
            value = value + float(r[propName])
        #gold   = float(r['gold'])
        #satisfaction = float(r['satisfaction'])
        #combined = 10*(hope + 1.1*joy + 1.5*satisfaction)
        if map[(y,x)]==white:
           map[(y,x)] = value
        else:
           map[(y,x)] = max(map[(y,x)],value)

    ax = plt.gca()
    ax.xaxis.set_visible(False)
    ax.yaxis.set_visible(False)
    plt.imshow(map, cmap='hot', origin='lower', interpolation='nearest')
    #plt.imshow(map, cmap='hot', origin='lower', interpolation='bilinear')

    plt.title("heat map")
    #plt.legend()
    if saveToFile : plt.savefig(scriptOptions.outputFile)
    else : plt.show()

def main():
    ## Parsing the command-line argumentrs
    parseCommandLineArgs()
    print('** Input file: ', scriptOptions.inputFile)
    print('** Graph type: ', scriptOptions.graphType)
    print('** Properties to show: ', selectedProperties)

    #print(content)
    if(scriptOptions.graphType=="timegraph") :
        mkTimeProgressionGraph(scriptOptions.inputFile)
    elif(scriptOptions.graphType=="heatmap") :
        mkHeatMap(scriptOptions.inputFile,
            int(scriptOptions.heatmapWidth),
            int(scriptOptions.heatmapHeight),
            float(scriptOptions.heatmapSizeScale),
            float(scriptOptions.heatmapValMin),
            float(scriptOptions.heatmapValMax)
            )


if __name__ == "__main__":
   #main()
   prop = ["aa", "bb", "cc"]
   dict = {}
   i = 0
   for k in prop:
      dict[k] = i
      i = i + 1
   print(f"** {dict}")
