# TrackUser
ECE438 HW2. Users are tracked. Their steps are counted and their rotation from the initial orientation is measured.

Complementary filter logic can be found here: http://www.thousand-thoughts.com/2012/03/android-sensor-fusion-tutorial/comment-page-2/

Getting orientation with accelerometer + magnetometer 
http://stackoverflow.com/questions/20339942/android-get-device-angle-by-using-getorientation-function


To run the matlab files:
Copy the files into the same folder, change the file path in importfile.m to whatever .csv file you want to load and then Run the following commands:
importfile; convertSteps; deadRec; plotData;
Plot data is only necessary, if you want the magnetometer vs. gyroscope plot. deadRec gives you the plot of the walked path.
