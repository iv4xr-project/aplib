#!/usr/local/bin/python3

saveToFile = True
import sys
from optparse import OptionParser
import matplotlib
if saveToFile:
   matplotlib.use('Agg')   # to generate png output, must be before importing matplotlib.pyplot
import matplotlib.pyplot as plt
import matplotlib.patches as mpatches
import matplotlib.ticker as ticker
from mpl_toolkits.mplot3d import Axes3D
import numpy as np
import math
import csv
import os
import pprint
import statistics
import scipy.stats as scistats


def loadCSV(csvfile):
   """ A help function to read a csv-file. """
   # need to use a correct character encoding.... latin-1 does it
   with open(csvfile, encoding='latin-1') as file:
      content = csv.DictReader(file, delimiter=',')
      rows = []
      for row in content: rows.append(row)
      return rows

def mk3DHeatMap(filename,width,length,depth,game):
    dataset = loadCSV(filename)
    map = np.zeros((width,length,depth))
    #for y in range(0,height):
    #  for x in range(0,width):
    #      map[y][x] = -1

    for r in dataset:
        x = round(float(r['posx']))
        y = round(float(r['posz']))
        h = round(float(r['posy']))
        map[x][y][h] = map[x][y][h] + 1
    
    fig = plt.figure()
    ax = fig.add_subplot(111, projection='3d')
    ax.set_xlim(0,width)
    ax.set_ylim(0,length)
    ax.set_zlim3d(0,depth-1)

    # control scaling and ticking:
    if game=="MD":
       ax.xaxis.set_major_locator(ticker.MultipleLocator(3))
       ax.yaxis.set_major_locator(ticker.MultipleLocator(3))
    else: # else game is SE
       ax.xaxis.set_major_locator(ticker.MultipleLocator(10))
       ax.yaxis.set_major_locator(ticker.MultipleLocator(10))
    ax.zaxis.set_major_locator(ticker.MultipleLocator(1))
    # adjusting the spacing of the z-axis:
    if game=="MD":
       ax.get_proj = lambda: np.dot(Axes3D.get_proj(ax), np.diag([1,1,1,1]))
    else:   
       ax.get_proj = lambda: np.dot(Axes3D.get_proj(ax), np.diag([1,1,0.4,1]))

    # Create 3D heatmap
    x,y,h = map.nonzero() # this gets the non-zero indices?
    ax.scatter(x, y, h, c=map[x,y,h], cmap='hot', marker='s', s=100, alpha=0.4)

    #ax.scatter(x, y, h, c=map[x,y,h], cmap='viridis', alpha=0.6)

    if game=="MD" : ax.set_zlabel('maze-nr')
    
    # changing z-ticks to prepend with word "maze" ... well does not work :(
    #zlabels = ax.get_zticklabels()  
    #for label in zlabels:
    #    #_,myZ = label.get_position()  # extract the y tick position
    #    txt0 = label.get_text()  # extract the text
    #    txt = f'  maze {txt0}'       # update the text string
    #    label.set_text(txt0)     # set the text
    #ax.set_zticklabels(zlabels) 

    plt.title("heat map")
    if saveToFile : plt.savefig("heatMap.png")
    plt.show()

#def main():
#    mk3DHeatMap("visits-2mazes-type1-h1-0 targeted.csv",20,20,2)

if __name__ == "__main__":
   game = sys.argv[1]
   filename = sys.argv[2]
   
   # for SE
   # hardcoding level size to 80x80, and height 10:
   if game=="SE":
      mk3DHeatMap(filename,80,80,10,game)
   # for MD:
   # hardcoding level size to 20x20, and two mazes:
   else:
      mk3DHeatMap(filename,20,20,2,"MD")