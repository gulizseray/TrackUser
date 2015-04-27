dT = diff(timestamp)/1000;

dAlpha1 = rot_z(2:end) .* dT;
alpha1 = cumsum(dAlpha1) * 180/pi;
totalRotation1 = cumsum(abs(dAlpha1))*180/pi;
error1 = min(abs([360-alpha1, 270-alpha1, 180-alpha1, 90-alpha1, alpha1, alpha1+90, alpha1+180, alpha1 + 270, alpha1 + 360]), [],2);

acc_parallel = gravity([rot_x, rot_y, rot_z], [acc_x, acc_y, acc_z]);
acc_parallel = acc_parallel(2:end);

dAlpha2 = dAlpha1;
% dAlpha2(abs(rot_z(2:end))<0.15) = 0;
% %dAlpha2(abs(acc_parallel)<8) = 0;
% alpha2 = cumsum(dAlpha2)*180/pi;

[maxi, t_max] = findpeaks(cumsum(dAlpha2));
%t_max = timestamp(t_max+1);
[mini, t_min ] = findpeaks(-cumsum(dAlpha2));
mini = -mini;
%t_min = timestamp(t_min+1);

peaks = zeros(length(maxi) + length(mini), 1);

if t_min(1) < t_max(1)
    peaks(1:2:end) = mini;
    peaks(2:2:end) = maxi;
else
    peaks(2:2:end) = mini;
    peaks(1:2:end) = maxi;
end

time_p = sort([t_max; t_min]);

% for k=1:length(t_max)
%    if abs(maxi(k) - mini(k)) < 0.3142
%        dAlpha2(t_max(k):t_min(k)) = 0;
%    end
% end
% 
% for k=2:lenght(t_max)
%    if abs(maxi(k) - mini(k-1) < 0.3142
%        dAlpha2(t_min(k-1):t_max(k)
% end
noCC = 0;
detectionTH = 0.1745;
cd = 0;

for k=2:length(time_p)
    detectionTH = detectionTH + 0.0175;
    detectionTH = min(0.1745, detectionTH);
    if detectionTH == 0.1745
        cd = 0;
    end
    
    d = peaks(k) - peaks(k+-1);
   if abs(d)>0.1745
       detectionTH = 0; %0.0873;
       cd = sign(d);
   elseif d>0 && d < detectionTH && cd ~= -1
       dAlpha2(time_p(k-1):time_p(k)) = 0;
   elseif d<0 && d > -detectionTH && cd ~=1
        dAlpha2(time_p(k-1):time_p(k)) = 0;  
   end
end

 dPeaks = peaks(2:end) - peaks(1:end-1);

alpha2 = cumsum(dAlpha2)*180/pi;


totalRotation2 = cumsum(abs(dAlpha2))*180/pi;
error2 = min(abs([360-alpha2, 270-alpha2, 180-alpha2, 90-alpha2, alpha2, alpha2+90, alpha2+180, alpha2+270, alpha2+360]),[],2);



figure;
hold on; title('Alpha');
plot(timestamp(2:end), [alpha1, error1, alpha2, error2]);
plot(timestamp(time_p), peaks*180/pi ,'x');
legend('without th', 'error w/o', 'with th', 'error w');
grid minor;

figure;
hold on; title('Total Rotation');
plot(timestamp(2:end), [totalRotation1, totalRotation2]);
legend('without th', 'with th');
grid minor;

%%Plot of the whole path
figure; hold on;

%This is the angle measured by the smartphone
x_position = zeros(length(timestamp)-1,1);
y_position = zeros(length(timestamp)-1,1);
for k = 2:length(timestamp)-1
   x_position(k) = x_position(k-1) - sin(fusedAngle(k)) * (distance(k) - distance(k-1));
   y_position(k) = y_position(k-1) + cos(fusedAngle(k)) * (distance(k) - distance(k-1));
end
plot(x_position, y_position); 

%This is the purely dead-reckoned angle
x_position = zeros(length(timestamp)-1,1);
y_position = zeros(length(timestamp)-1,1);
for k = 2:length(timestamp)-1
   x_position(k) = x_position(k-1) - sin(alpha2(k)*pi/180) * (distance(k) - distance(k-1));
   y_position(k) = y_position(k-1) + cos(alpha2(k)*pi/180) * (distance(k) - distance(k-1));
end
plot(x_position, y_position); 

%This is the dead-reckoned angle after turn detection
x_position = zeros(length(timestamp)-1,1);
y_position = zeros(length(timestamp)-1,1);
for k = 2:length(timestamp)-1
   x_position(k) = x_position(k-1) - sin(alpha1(k)*pi/180) * (distance(k) - distance(k-1));
   y_position(k) = y_position(k-1) + cos(alpha1(k)*pi/180) * (distance(k) - distance(k-1));
end
hold on;
plot(x_position, y_position); 
legend('from phone', 'original' ,'filtered'); grid minor;

axis equal;