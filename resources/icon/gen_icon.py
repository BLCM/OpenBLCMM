#!/usr/bin/env python
# vim: set expandtab tabstop=4 shiftwidth=4:

# Copyright (C) 2023 Christopher J. Kucera
# <cj@apocalyptech.com>
# <https://apocalyptech.com/contact.php>
#
# OpenBLCMM is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program.  If not, see <http://www.gnu.org/licenses/>

import os
import re
import sys
import enum
import argparse

# This is an embarrassingly unnecessary and absurdly overengineered
# script to generate the OpenBLCMM app icon.  The icon was originally
# made by hand in Gimp (https://www.gimp.org/), and later converted
# over to SVG with Inkscape (https://inkscape.org/) using the Gimp
# paths (and tweaked again), but at high resolutions there were
# some obvious flubs which I sort of wanted to clean up.  They weren't
# apparent when scaled down to the actual app-usage sizes, but it
# clearly bothered me enough to write this thing.  I figured the
# easiest way to ensure total uniformity would be to generate it with
# code, so here we are.
#
# I'm sure there are plenty of nice Python SVG libraries I could've
# used, but instead I just took the Inkscape-generated file and
# started replacing bits.  I stripped out a lot of the Inkspace-specific
# namespace but kept the older "sodipodi" attributes which, among
# other things, define some control defaults for the nodes.
#
# I also figured that since I was going to the trouble of doing this
# at all, I may as well parameterize as much as possible, so the script
# can be used to generate variants of the icon.  Yay?

class NodeType(enum.Enum):
    """
    Inkscape node type codes:
        c = cusp
        s = smooth
        z = symmetric
        a = auto-smooth

    Turns out that given how we're encoding this thing, there's
    really no need for anything but Cusp, so we could've done away
    with this abstraction.  But whatever, it's already in place
    so I'm leaving it in.
    """
    CUSP = 'c'
    SMOOTH = 's'
    SYM = 'z'
    AUTO = 'a'

class Transform(enum.Enum):
    """
    Transforms we can apply to a Coord
    """
    ROT_0 = enum.auto()
    ROT_90 = enum.auto()
    ROT_180 = enum.auto()
    ROT_270 = enum.auto()
    FLIP_H = enum.auto()

class Coord:
    """
    Little helper class to handle coordinates, mostly just so I can
    rotate/flip them at will, and render them with the apparent
    Inkscape default of five decimal place rounding.
    """

    def __init__(self, x, y):
        self.x = x
        self.y = y

    def __eq__(self, other):
        return self.x == other.x and self.y == other.y

    def __repr__(self):
        return f'Coord<{round(self.x, 5)},{round(self.y, 5)}>'

    def transform(self, trans, size):
        match trans:
            case Transform.ROT_0:
                return self
            case Transform.ROT_90:
                return Coord(size-self.y, self.x)
            case Transform.ROT_180:
                return Coord(size-self.x, size-self.y)
            case Transform.ROT_270:
                return Coord(self.y, size-self.x)
            case Transform.FLIP_H:
                return Coord(size-self.x, self.y)
            case _:
                raise RuntimeError(f'Unknown transform: {trans}')

    def render(self):
        return ','.join([
                str(round(self.x, 5)),
                str(round(self.y, 5)),
                ])

class Node:
    """
    A generic node in the SVG path.  Note that we're only supporting
    absolute coordinates here; relative just didn't seem worth it.
    """

    def __init__(self, command, node_type):
        self.command = command.upper()
        self.node_type = node_type

    def render(self, prev_node=None):
        parts = []
        if prev_node is None or prev_node.command != self.command:
            parts.append(self.command)
        parts.extend(self._command_parts())
        return ' '.join(parts)

    def _command_parts(self):
        return []

class Move(Node):
    """
    A "move" command in the SVG path
    """

    def __init__(self, node_type, coord):
        super().__init__('m', node_type)
        self.coord = coord

    def _command_parts(self):
        parts = []
        parts.append(self.coord.render())
        return parts

class Line(Node):
    """
    A "line" command in the SVG path
    """

    def __init__(self, node_type, coord):
        super().__init__('l', node_type)
        self.coord = coord

    def _command_parts(self):
        parts = []
        parts.append(self.coord.render())
        return parts

class Curve(Node):
    """
    A Curve command in the SVG path
    """

    def __init__(self, node_type, coord, start, end):
        super().__init__('c', node_type)
        self.coord = coord
        self.start = start
        self.end = end

    def _command_parts(self):
        parts = []
        parts.append(self.start.render())
        parts.append(self.end.render())
        parts.append(self.coord.render())
        return parts

class Finish(Node):
    """
    A Finish command in the SVG path
    """

    def __init__(self, node_type):
        super().__init__('z', node_type)

class Arch:
    """
    Abstracted class to do math related to drawing our arches, since we
    draw two separate ones with slightly different parameters.
    """

    def __init__(self, bottom_y, ratio, mid_pct, width, bisect_x):
        """
        Initializes a new Arch.  There's five total nodes - two on each
        side plus one at the peak of the arch.  The two on each side are
        mirror images of each other.  One of those is a point on the
        "floor", which then draws a straight line up to the "mid" point.
        Then the midpoint draws a curve up to the peak.

        `bottom_y` is the Y value of the "floor" where the arch sits.
        `ratio` is the ratio of width-to-height for the arch.  By default
            this is gonna be <1 because they're taller than they are wide.
        `mid_pct` is a tuple describing where to draw the midpoint.  It's
            specified in percentages along x+y from the "peak" point to
            the bottom point.
        `width` is the total width of the base, from bottom-point to
            bottom-point.
        `bisect_x` is the x value of the peak node (ie: right in the middle
            of the icon)
        """
        self.bottom_y = bottom_y
        self.ratio = ratio
        self.mid_pct = mid_pct
        self.width = width
        self.bisect_x = bisect_x

        # Now some calculations
        self.top_y = self.bottom_y - (self.width/self.ratio)
        self.height = self.top_y - self.bottom_y
        self.bottom = Coord(
                self.bisect_x + (self.width/2),
                self.bottom_y
                )
        self.mid = Coord(
                self.bisect_x + (self.width/2)*self.mid_pct[0],
                self.top_y - (self.height*self.mid_pct[1])
                )
        self.top = Coord(
                self.bisect_x,
                self.top_y,
                )

        # Getting a concise and understandable parameter for getting the top of
        # the arch to look right is a bit difficult; the "roundness" parameter
        # here doesn't actually apply the same way to all nodes.  For the top
        # node we're going out by a third, but for the "mid" nodes we're going
        # up by *two-thirds*.  I dunno.  This value feels pretty good, though.
        # It's technically slightly "wider" than the original hand-drawn copy
        # but eh.
        arch_roundness = 0.33
        self.slope = (self.bottom.x-self.mid.x) / (self.bottom.y-self.mid.y)
        mid_cp_dest_y = self.top.y + ((self.mid.y-self.top.y)*arch_roundness)
        self.mid_control_point = Coord(
                self.mid.x - ((self.mid.y-mid_cp_dest_y)*self.slope),
                mid_cp_dest_y,
                )
        self.top_control_point = Coord(
                self.top.x + ((self.mid.x-self.top.x)*arch_roundness),
                self.top.y,
                )

    def render(self, nodes, size, inline):
        if inline:
            nodes.append(Line(NodeType.CUSP, self.bottom))
        else:
            nodes.append(Move(NodeType.CUSP, self.bottom))
        nodes.append(Line(NodeType.CUSP, self.mid))
        nodes.append(Curve(NodeType.CUSP,
                self.top,
                self.mid_control_point,
                self.top_control_point,
                ))
        nodes.append(Curve(NodeType.CUSP,
                self.mid.transform(Transform.FLIP_H, size),
                self.top_control_point.transform(Transform.FLIP_H, size),
                self.mid_control_point.transform(Transform.FLIP_H, size),
                ))
        nodes.append(Line(NodeType.CUSP,
                self.bottom.transform(Transform.FLIP_H, size)))
        if not inline:
            nodes.append(Finish(NodeType.CUSP))

class HexColorAction(argparse.Action):
    """
    Custom argparse action to enforce color hex values
    """

    def __call__(self, parser, namespace, values, option_string=None):
        if not re.match(r'^[0-9a-fA-F]{6}$', values):
            parser.error('argument {}: must be a valid hex color string'.format(
                '/'.join(self.option_strings),
                ))
        setattr(namespace, self.dest, values)

class BetweenZeroAndNearlyHalfAction(argparse.Action):
    """
    Custom argparse action to enforce floats between 0 <= x < 0.5
    """

    def __call__(self, parser, namespace, values, option_string=None):
        values = float(values)
        if values < 0 or values >= 0.5:
            parser.error('argument {}: must be in the range 0 <= x < 0.5'.format(
                '/'.join(self.option_strings),
                ))
        setattr(namespace, self.dest, values)

class BetweenZeroAndOneAction(argparse.Action):
    """
    Custom argparse action to enforce floats between 0 <= x <= 1
    """

    def __call__(self, parser, namespace, values, option_string=None):
        values = float(values)
        if values < 0 or values > 1:
            parser.error('argument {}: must be in the range 0 <= x <= 1'.format(
                '/'.join(self.option_strings),
                ))
        setattr(namespace, self.dest, values)

class BetweenNearlyZeroAndOneAction(argparse.Action):
    """
    Custom argparse action to enforce floats between 0 < x <= 1
    """

    def __call__(self, parser, namespace, values, option_string=None):
        values = float(values)
        if values <= 0 or values > 1:
            parser.error('argument {}: must be in the range 0 < x <= 1'.format(
                '/'.join(self.option_strings),
                ))
        setattr(namespace, self.dest, values)

def main():

    ###
    ### Args
    ###

    # TODO: Should we support setting the middle gradient offset point?

    parser = argparse.ArgumentParser(
            description='Generates an OpenBLCMM logo SVG',
            formatter_class=argparse.ArgumentDefaultsHelpFormatter,
            )

    parser.add_argument('-o', '--output',
            type=str,
            default='openblcmm_icon.svg',
            help='Output filename',
            )

    parser.add_argument('-s', '--size',
            type=int,
            default=1512,
            help='"Native" size of the icon',
            )

    parser.add_argument('-t', '--thickness',
            type=int,
            default=51,
            help="Stroke thickness",
            )

    parser.add_argument('-c', '--color',
            action=HexColorAction,
            default='ff4f4f',
            help="Stroke color, as a hex value.",
            )

    parser.add_argument('-m', '--margin-pct',
            action=BetweenZeroAndNearlyHalfAction,
            default=0.021,
            help="""Percentage of the total area which is a margin.  This
                does *not* take stroke thickness into account.  Valid values
                are 0 <= x < 0.5, though only real small values make sense.""",
            )

    parser.add_argument('--corner-size-pct',
            action=BetweenZeroAndNearlyHalfAction,
            default=0.12,
            help="""Percentage of the icon width (minus margins) which is
                curved at the corners.  A value of 0.5 would result in a perfect
                circle with no arch; valid values are 0 <= x < 0.5.""",
            )

    parser.add_argument('--corner-roundness',
            action=BetweenZeroAndOneAction,
            default=0.55,
            help="""How round to make the corners.  A value of 0 will result in
                a sharp diagonal.  Valid values are 0 <= x <= 1.""",
            )

    parser.add_argument('--outer-arch-size-ratio',
            type=float,
            default=0.86,
            help="""Ratio of width-to-height for the outer arch""",
            )

    parser.add_argument('--outer-arch-width-pct',
            action=BetweenNearlyZeroAndOneAction,
            default=0.97,
            help="""Percentage of the bottom straight edge which is taken up
                by the vault arch.  Valid values are 0 < x <= 1.""",
            )

    parser.add_argument('--inner-arch-size-ratio',
            type=float,
            default='0.61',
            help="""Ratio of width-to-height for the inner arch""",
            )

    parser.add_argument('--inner-arch-width-pct',
            action=BetweenNearlyZeroAndOneAction,
            default=0.63,
            help="""Percentage difference in width between the outer arch
                and the inner arch.  Valid values are 0 < x <= 1.""",
            )

    parser.add_argument('--gradient-bottom',
            action=HexColorAction,
            default='1b1b1b',
            help="Bottom gradient color, as a hex value.",
            )

    parser.add_argument('--gradient-middle',
            action=HexColorAction,
            default='585858',
            help="Middle gradient color, as a hex value.",
            )

    parser.add_argument('--gradient-top',
            action=HexColorAction,
            default='7c7c7c',
            help="Top gradient color, as a hex value.",
            )

    args = parser.parse_args()

    ###
    ### Some other values we can tweak which would be too weird for CLI args.
    ### These determine exactly when the arches start curving.
    ###

    # Outer arch midpoint percent, inbetween top midpoint and base
    arch_outer_mid_pct = (0.40, 0.14)

    # Inner arch midpoint percent, inbetween top midpoint and base
    arch_inner_mid_pct = (0.29, 0.07)

    ###
    ### Start doing some math and then process
    ###

    # Some computations
    margin = args.size*args.margin_pct
    inner_size = args.size-(margin*2)
    border_start = inner_size*args.corner_size_pct
    gradient_x1 = round(args.size/2, 5)
    gradient_y1 = round(args.size-margin, 5)
    gradient_x2 = round(args.size/2, 5)
    gradient_y2 = round(margin, 5)

    # Rounded border coords, based on upper left
    border_line_start = Coord(margin+border_start, margin)
    border_curve_start = Coord(args.size-margin-border_start, margin)
    border_curve_end = Coord(args.size-margin, margin+border_start)
    border_curve_control_start = Coord(
            args.size-margin-(border_start*(1-args.corner_roundness)),
            margin
            )
    border_curve_control_end = Coord(
            args.size-margin,
            margin+(border_start*(1-args.corner_roundness))
            )

    # Outer arch
    arch_bottom_y = args.size-margin
    arch_outer = Arch(arch_bottom_y,
            args.outer_arch_size_ratio,
            arch_outer_mid_pct,
            (inner_size-(border_start*2))*args.outer_arch_width_pct,
            args.size/2,
            )

    # Inner Arch calculations
    arch_inner = Arch(arch_bottom_y,
            args.inner_arch_size_ratio,
            arch_inner_mid_pct,
            arch_outer.width*args.inner_arch_width_pct,
            args.size/2,
            )

    # Starting to construct
    nodes = []
    for trans in [
            Transform.ROT_0,
            Transform.ROT_90,
            Transform.ROT_180,
            Transform.ROT_270,
            ]:
        
        # Transform the border coords appropriately
        t_border_line_start = border_line_start.transform(trans, args.size)
        t_border_curve_start = border_curve_start.transform(trans, args.size)
        t_border_curve_end = border_curve_end.transform(trans, args.size)
        t_border_curve_control_start = border_curve_control_start.transform(trans, args.size)
        t_border_curve_control_end = border_curve_control_end.transform(trans, args.size)

        # If we're just starting, move to the starting position
        if not nodes:
            nodes.append(Move(NodeType.CUSP, t_border_line_start))

        # Do the outer arch stuff, if we need to!
        if trans == Transform.ROT_180:
            arch_outer.render(nodes, args.size, True)

        # Now finish out the straight line (or just do the whole thing)
        nodes.append(Line(NodeType.CUSP, t_border_curve_start))

        # Now draw the curve
        nodes.append(Curve(NodeType.CUSP,
                t_border_curve_end,
                t_border_curve_control_start,
                t_border_curve_control_end,
                ))

    # Finish up the path
    # Note that the SVG Close Path command does *not* have any params to
    # specify bezier control points, so unless the "end" point is directly
    # on top of the "start" point, the last segment can only be a straight
    # line.  So, I've just made sure to end it on a straight line.
    nodes.append(Finish(NodeType.CUSP))

    # Write out the inner arch, too
    arch_inner.render(nodes, args.size, False)

    # Render
    path_commands = ' '.join([n.render() for n in nodes])
    node_types = ''.join([n.node_type.value for n in nodes])

    with open(args.output, 'w') as odf:
        odf.write(f'''<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<svg
   width="{args.size}"
   height="{args.size}"
   viewBox="0 0 {args.size} {args.size}"
   version="1.1"
   id="SVGRoot"
   sodipodi:docname="{args.output}"
   xml:space="preserve"
   xmlns:sodipodi="http://sodipodi.sourceforge.net/DTD/sodipodi-0.dtd"
   xmlns:xlink="http://www.w3.org/1999/xlink"
   xmlns="http://www.w3.org/2000/svg"
   xmlns:svg="http://www.w3.org/2000/svg"><sodipodi:namedview
     id="namedview14"
     pagecolor="#ffffff"
     bordercolor="#666666"
     borderopacity="1.0"
     showgrid="true" /><defs
     id="defs9"><linearGradient
       id="linearGradient4109"><stop
         style="stop-color:#{args.gradient_bottom};stop-opacity:1;"
         offset="0"
         id="stop4113" /><stop
         style="stop-color:#{args.gradient_middle};stop-opacity:1;"
         offset="0.76206195"
         id="stop14282" /><stop
         style="stop-color:#{args.gradient_top};stop-opacity:1;"
         offset="1"
         id="stop4115" /></linearGradient><linearGradient
       xlink:href="#linearGradient4109"
       id="linearGradient4111"
       x1="{gradient_x1}"
       y1="{gradient_y1}"
       x2="{gradient_x2}"
       y2="{gradient_y2}"
       gradientUnits="userSpaceOnUse" /></defs><g
     id="layer1"><path
       id="Combined"
       fill="none"
       stroke="#000000"
       stroke-width="1"
       d="{path_commands}"
       style="fill:url(#linearGradient4111);fill-opacity:1;fill-rule:evenodd;stroke:#{args.color};stroke-width:{args.thickness};stroke-linecap:round;stroke-linejoin:miter;stroke-dasharray:none;stroke-opacity:1"
       sodipodi:nodetypes="{node_types}" /></g></svg>''')

        print(f'Wrote to: {args.output}')

if __name__ == '__main__':
    main()

