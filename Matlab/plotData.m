%% This script calculates and plots Data about the dead reckoned path.

%% Import the Data from the .csv file into Matlab workspace and interpolate
% steps to get an estimate for the distance inbetween two steps
importFile;
convertSteps;

%compass = pi - atan2(mag_y, mag_x);

%% Calculate and plot the difference between meassured angle and compass 
difference = fusedAngle - compass;
difference(difference>pi) = difference(difference>pi) - 2*pi;
difference(difference<-pi) = difference(difference<-pi) + 2*pi;

figure; 
plot(timestamp/1000, [fusedAngle, compass]*180/pi);%, '.', 'MarkerSize', 8);
title(strcat('Measured values and their difference: ', filename), 'Interpreter' ,'none');
xlabel('time/s'); ylabel('angle/°');
legend('Measured angle', 'Magnetic angle');
grid minor;

% 
% %% Calculate and plot the dead reckoned path
% x_position = zeros(length(timestamp),1);
% y_position = zeros(length(timestamp),1);
% for k = 2:length(timestamp)
%    x_position(k) = x_position(k-1) - sin(fusedAngle(k)) * (distance(k) - distance(k-1));
%    y_position(k) = y_position(k-1) + cos(fusedAngle(k)) * (distance(k) - distance(k-1));
% end
% 
% figure; 
% plot(x_position, y_position);
% title(strcat('Walked path: ', filename), 'Interpreter' ,'none'); xlabel('x/m'); ylabel('y/m');
% grid minor; axis equal;
% 
% %% Plot steps per second
% figure;
% plot(timestamp/1000, sps);
% title(strcat('Steps per second - Mean: ', num2str(mean(sps),3), ' - ', filename), 'Interpreter', 'none');
% xlabel('time/s'); ylabel('steps per second');
% grid minor;