<small><small>[Back to Index](../../../index.md)</small></small>

## Further Features: center automatically current gps position

### Static behaviour

By default this feature is switched on. This means that for every new GPS 
position the map will be aligned in a way that the new position is in the center
of the map.

The following sequence shows the alignment of the map with each newly detected position:

<img src="./center1.png" width="200" />&nbsp;
<img src="./center2.png" width="200" />&nbsp;
<img src="./center3.png" width="200" />&nbsp;
<img src="./center4.png" width="200" />&nbsp;

This is a usually a nice feature since it's not necessary to align the map 
manually to the current position. But if you want to check some other area
e.g. to verify the route, it might be annoying if you scroll to a certain position
and then the app jumps back due to a new location from GPS sensor. Therefore this
feature allows to disable/enable this function to automatically center GPS position.

Use <img src="../../../icons/group_record2.svg" width="24"/> + <img src="../../../icons/center1.svg" width="24"/> to switch the feature off.

If you move further, then the screen isn't anymore aligned to new GPS locations

<img src="./center5.png" width="200" />&nbsp;
<img src="./center6.png" width="200" />&nbsp;

As soon as you switch the feature again on
(<img src="../../../icons/group_record2.svg" width="24"/> + <img src="../../../icons/center2.svg" width="24"/>), the screen will be aligned
to the last GPS position.

<img src="./center7.png" width="200" />&nbsp;

### Dynamic behaviour

If you just want to have a short lookup aside your current position, then you don't have to switch the feature off. If you move
the map, it will not center a new position within the next 7 seconds. If you move the map within this 7 seconds again, the timer will
start with another 7 seconds again.
As soon as the time expires, the last gps position is put to the center again.
There is also the option to tap in the center of the screen (as marked with the beeline feature). In this case the map is
moved to the last gps position immediately.

There is another exception. If you want to create a route while you are recording, then you want to stay with the map in the area where you are planing
and you don't want that the map is centered to your current position. Therefrore as long as the route edit mode is switched on, the automatic center funcion is disabled. 

<small><small>[Back to Index](../../../index.md)</small></small>