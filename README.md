# FireHack3

Solution to the Swarm and Search AI challenge that took place 29th - 31st March 2019.
This program is built to work with the AMASE simulation software.

Link to hackathon details page: https://fire-hack.devpost.com/

## Overview of Problem

In this hackathon, we were given a series of scenarios. Each scenario consists of one or more drones, and one or more hazard zones, within a 3D environment.
Knowledge of the scenario is only known through information returned by the drones.
The goal of the challenge is to accurately map the hazard zone boundary, over time.

Therefore there are three high-level problems:

1. Control of drones to efficiently search an unknown space for hazard zone edges.
2. Control of drones to accurately map the boundary of a hazard zone, given one or more of its edges.
3. Distribution of resources between the previous two problems.

## Overview of Solution

Our solution uses a series of two-dimensional numeric arrays, known as maps.
Each map corresponds to a different resolution, much like the graphics setting on a video.
Larger maps are capable of holding more detailed information about the environment.

Each position in each array holds an attention weight determining how important it is for information at that position to be revealed.
This information is updated using a hardcoded protocol that reads in input data, and updates the various arrays.
If information at a position is revealed, then its attention weight is set to 0.
Attention weights for all positions increases slowly over time, but especially for those nearby to known hazard positions.
A separate list holds all the known hazard points.

Resolution maps are necessary because you don't want to search an unknown space in detail.
Rather, you want to quickly scan it at a few points, and determine whether a hazard zone exists.
Hazard zones are large enough for this approach to reliably work.
However for places where hazard zones are known to exist, you want to look in detail and precisely map out the boundary.

The Control class uses information from these maps to determine the next commands of each of the drones.
Each drone is at any one point in time assigned to one resolution.
Each drone has an attention weight to every position on the map of its assigned resolution.
The weight is increased by distance from drone, and decreased by distance to other drones.
Using this system, drones can distribute themselves efficiently over a target space.

When a hazard zone boundary is detected, a protocol assigns a drone to map the edges.
Drones are assigned based on distance and drone type.
When assigned, a drone typically is reassigned to a map with a greater resolution.
A seperate protocol distributes the resolution assignments based on attention weights at each resolution.

## Quality of Solution

The solution performs well with respect to the first problem, that of searching an unknown space.
However for the second problem, the approach to stare at individual points to map a hazard zone boundary is not fast enough.
Most other teams decided to use a sweeping camera approach that performs much better.

## Solution Issues

A serious bug can cause the program to fail when a hazard point is encountered for the first time, this tends to occur in larger scenarios.

The individual drone attention weights for searching are not optimised to properly balance between distance and raw map attention.

## Software Limitations

The AMASE simulation software has a serious flaw that, in my opinion, render it a poor model of the real-world problem:
Drones can only observe at a single discrete point, rather than a cone of view.
This limitation means that even when a drone encounters a hazard boundary, instead of seeing an edge, it sees a single point.
Therefore the greatest challenge in this event was not effectively controlling a swarm, but rather micro-managing each drones actions so that it could maintain effective visual distance and position from the hazard boundary.
