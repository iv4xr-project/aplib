/**
 * This package provides classes to do pathfinding over a graph. It contains
 * classes such as:
 * 
 *   <ol>
 *   <li> {@link AStar} implementing the A* graph-pathfinding algorithm. The
 *   target graph can be anything that implement the  {@link Navigatable} interface.
 *   
 *   <li> {@link Navigatable} is an interface for defining a graph that is
 *   searchable by the pathfinder(s) provided by this package.
 *   
 *   <li> {@link SimpleNavGraph} is an implementation of {@link Navigatable} that facilitates
 *   pathfinding over a surface defined by a surface-mesh.
 *   
 *   <li> {@link SurfaceNavGraph} is an extension of {@link SimpleNavGraph} that
 *   additionally provides support for surface exploration.
 *   
 *   </ol>
 *   
 */
package eu.iv4xr.framework.extensions.pathfinding;