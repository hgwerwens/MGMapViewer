<small><small>[Back to Index](../../../index.md)</small></small>

## Further Features: status line
 
The status line is always visible, but it is kept semi transparent to have an idea about the 
underlying map part. The status line can shows following information:
- [air distance](../BeelineDistance/beeline.md) between current position and a point of interest
- zoom level of the current map 
- current time
- running background jobs
- current elevation
- [remaining distance](../Remaining/remaining.md) along the selected track
- battery level

While the zoom level, the time and the battery level information are always available,
the air distance, the number of background jobs, the current elevation and the remaining distance information are only 
available if a certain context is given. These fields are set visible only, if they can provide 
some meaningful information. On the other hand there is only enough space for five out of this 
seven fields. Therefore in the rare case, that for more fields information is available, 
only the first 5 item are visualized.

The following figures illustrate this behaviour:

<img src="./stat2.png" width="400" />&nbsp;
<img src="./stat1.png" width="400" />&nbsp;

<small><small>[Back to Index](../../../index.md)</small></small>