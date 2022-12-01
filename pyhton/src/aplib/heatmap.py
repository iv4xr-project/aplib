#!/usr/local/bin/python3

# A script to construct a heatmap from values recorded in an csv-input file. 
# Each row in the file is seen as a visit to some 2D-location (x,y). So the
# row must have information about the x and y it visits. Other (nummeric) data
# in the row represent various values sampled at the location (x,y). So,
# the whole input, which is a set of rows, can be seen as data over visits to
# some 2D-rectangle, which we will call a "map". From these visits, this script
# produces a "heatmap". 
#
# The map has some specified width and height. And imagine it is divided into
# square-tiles of size dxd. The size of tile is given by a parameter called "scale",
# and d = 1/scale. So, with scale=1, the squares will be of size 1x1. With scale=0.5
# the tiles will be of size 2x2.
#
# Simple heatmap
# ==============
# In the most simple form, a heatmap shows how frequent each tile is visited. A tile
# which is never visited will be displayed as white. A tile that is visited 1x is black,
# tiles that are visited more often red, and the more visits the brighter the color would
# be. Another parameter called valMax will cap the value of a tile to valMax.
#
# Heatmap over multiple properties.
# ==============
# If each row has other information/values than just x and y, you can specify a selection
# of property-names, e.g. p and q. If such a selection is specified, then the value of a
# tile is defined as the maximum of p+q over all visits to the tile in the dataset.
# If you want a different value-calculation, you can do this by re-programming the property
# combineFunction2 in the class HeatmapCommandLine.
#
# Heatmap from multiple csv-files
# ==============
# In the simple case, you use this script to produce a heatmap from a single csv-file. However
# it is also possible to specify a directory. In this case, all csv-files from the directory
# will be read and their data are appended. A heatmap is then produced from the combined data.
# (this implies that all those csv-files must have the same column-names, ordered in the same order)
# 
# Syntax
# ==============
#
#     > heatmap.py [options] arg1 arg2 ...
#
# If no arg is given, a visit-counting heatmap is produced. Else, the args is a list of property-names 
# whose maximum sum will be shown as the heatmap.
#
#  You can do "heatmap.py --help" to get some help-info. 
#
#  Example usages:
#
#     > heatmap.py -i input.csv  (produce a visit-counting heatmap)
#     > heatmap.py -dir=mydata   (produce a visit-counting heatmap from all csv-files in mydata)
#     > heatmap.py -i input.csv  health manna (produce a max-sum heatmap from the property health and manna)
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
from pathlib import Path
import pprint
import statistics
import scipy.stats as scistats
from mygraphlib import loadCSV

def mkHeatMap(dataset,
        selectedProperties,
        xLabel:str,
        yLabel:str,
        combineFunction,
        width:int,
        height:int,
        scale:float,
        maxvalue:float,
        outputfile:str="hmap"):
    """ A function to draw heatmap. 

    It takes data where every row represents a visit to some 2D location on a map of size
    width x height. Each row records the 2D position x,y it visits, and some properties, say u,v.
    The map can be thought to consist of tiles of size a x a, where a=1/scale. So, if scale=0.5
    the tiles are of size 2x2.

    To construct the heatmap, all tiles are initialized to the value 0. We then iterate over
    the rows in the dataset. A row r that visits to (x,y) is considered as a sample representing 
    the tile (p,q) where p = scale*x and q = scale*y. The value of tile(p,q) is then updated 
    by w = combineFunction(vold,s) where vold is the old value of tile(p,q), and s is the values of 
    selectedProperties in r. The value of w is also capped by maxvalue.

    Parameters
    -------------
    dataset: a list of rows. The first row specifies the column-names. Other rows consist of nummeric values.
    selectedProperties: name of the properties whose values are to be visualized in the heatmap.
    
    xLabel: the name of the column representing x-position, e.g. "x".
    
    yLabel: the name of the column representing x-position, e.g. "y".
    
    combineFunction: a function f(v,s) used to update the tiles' values.

    width: the map width.

    height: the map height.

    scale: the scale. This defines the size of the tiles, which is 1/scale.
        
    maxvalue: assumed max. value that can be assigned to a tile.

    outputfile: the name of the output file. Default is "hmap" (png).
    """

    #dataset = loadCSV(filename)
    H = math.ceil(scale*height)
    W = math.ceil(scale*width)
    map = np.zeros((H,W))
    for y in range(0,H):
      for x in range(0,W):
          map[y,x] = 0

    for r in dataset:
        x = round(scale*float(r[xLabel]))
        y = round(scale*float(r[yLabel]))

        # combine the value of the properties by summing them:
        values = [float(r[propName]) for propName in selectedProperties]
        map[y,x] = min(combineFunction(map[y,x],values) , maxvalue) # capping the val at max-value   

    # converting 0 to white:
    white = 1.25*maxvalue
    for y in range(0,H):
      for x in range(0,W):
          if map[y,x] == 0 : map[y,x] = white

    ax = plt.gca()
    ax.xaxis.set_visible(False)
    ax.yaxis.set_visible(False)
    # colormap, see: https://matplotlib.org/stable/tutorials/colors/colormaps.html
    plt.imshow(map, cmap='hot', origin='lower', interpolation='nearest', vmax=white)
    #plt.imshow(map, cmap='hot', origin='lower', interpolation='bilinear')

    plt.title("heat map")
    #plt.legend()
    if saveToFile : plt.savefig(outputfile)
    else : plt.show()

def visitCountCombineFunction(v,newvalues):
    '''
    A combine function that just sums/counts the visits.
    '''
    return v + 1

def maxSumCombineFunction(v,newvalues):
    '''
    A combine function that sums new-values, then maximizes it with the current cell.
    '''
    return max(v,sum(newvalues)) 

class HeatmapCommandLine:
    """
    A class implementing command-line interface to call the function mkHeatMap().
    The command line interface can read data from a single csv-file, or from
    all csv-files in a directory (their data will be appended).

    Attributes: 
    ---

    combineFunction2: combine-function to use when the set of selected properties
                      is not empty. The default is maxSumCombineFunction. You can 
                      change this if you want a different function.
    """

    def __init__(self):
        self.combineFunction1 = visitCountCombineFunction
        self.combineFunction2 = maxSumCombineFunction
    
    def parseCommandLineArgs(self):
        """ A help function to parse the command-line arguments of this script. """

        parser = OptionParser("usage: %prog [options] arg1 arg2 (each arg is a property-name/column-name in the inputfile)")
        parser.add_option("-i", dest="inputFile",  help="input csv-file (comma separated)")
        parser.add_option("--dir", dest="inputDir",  help="if specified, data will be read from all csv-files in this dir")  
        parser.add_option("-o", dest="outputFile", default="hmap", help="output file (png); default is hmap.png")
        parser.add_option("--width", dest="mapWidth" , help="the width of the heatmap")
        parser.add_option("--height", dest="mapHeight", help="the height of the heatmap")
        parser.add_option("--scale",  dest="tileScale", default="1",    help="the tile-scale of the heatmap")
        parser.add_option("--maxval", dest="mapValMax", default="100", help="maximum heat value in the heatmap")
        parser.add_option("--xname", dest="xName", default="x", help="label-name of x in the csv-file; default is \"x\"")
        parser.add_option("--yname", dest="yName", default="y", help="label-name of y in the csv-file; default is \"y\"")
        
        # parse the options and arguments:
        (options,args) = parser.parse_args()
        #print("xxx", options, "yyy", args)
        #print("a ", options.fileIn)
        if(options.inputFile == None):
            print("   Specify an input-file.")
            exit(2)
        return { "scriptOptions": options, "selectedProperties": args }  

    # the main function
    # It will use visitCountCombineFunction if no properties are specified,
    # else combineFunction2 above is used, which by default is defined as maxSumCombineFunction.
    #
    # If you want to use a different combineFunction2, you can change its def. above.
    #
    def main(self):
        ## Parsing the command-line argumentrs
        cmd = self.parseCommandLineArgs()
        scriptOptions = cmd["scriptOptions"]
        
        #print(content)
        if scriptOptions.inputDir == None :
            print('** Input file: ', scriptOptions.inputFile)
            dataset = loadCSV(scriptOptions.inputFile)
        else:
            dir = scriptOptions.inputDir
            print('** Input directory: ', dir)
            dataset = []
            k = 0
            for filename in os.listdir(dir):
                if(filename.endswith(".csv")):
                    file = Path(dir + "/" +  filename)
                    newdataset = loadCSV(file)
                    if k==0:
                        dataset = newdataset
                    else:
                        newdataset.pop(0)
                        dataset.extend(newdataset)
                    k = k+1

        selectedProperties = cmd["selectedProperties"] 
        if selectedProperties == [] :
            print('** Properties to show: - ')
            combineFunction = self.combineFunction1 # by default it is visitCountCombineFunction
        else :    
            print('** Properties to show: ', selectedProperties)
            combineFunction = self.combineFunction2  # by default it is maxSumCombineFunction

        mkHeatMap(dataset,
                selectedProperties,
                scriptOptions.xName,
                scriptOptions.yName,
                combineFunction,
                int(scriptOptions.mapWidth),
                int(scriptOptions.mapHeight),
                float(scriptOptions.tileScale),
                float(scriptOptions.mapValMax),
                scriptOptions.outputFile
                )


if __name__ == "__main__":
   cli = HeatmapCommandLine()
   cli.main()

   #dataset = loadCSV("samplePXTracefile.csv")
   #mkHeatMapWorker(dataset,{},"x","y",
   #      visitCountCombineFunction,90,70,0.5,5
   #   )

