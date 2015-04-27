time_s = -ones(length(timestamp), 1);
steps_s = -ones(length(timestamp), 1);

time_s(1) = timestamp(1);
steps_s(1) = steps(1);

for k = 2:length(timestamp)
    if steps(k) ~= steps(k-1)
        steps_s(k) = steps(k);
        time_s(k) = timestamp(k);
    end
end

time_s = time_s(time_s ~= -1);
steps_s = steps_s(steps_s ~=-1);
sps = 1000 * diff(steps_s)./diff(time_s);

distance = interp1(time_s, steps_s, timestamp) * 0.5;
sps = interp1(time_s(2:end), sps, timestamp);

sps(isnan(sps)) = 0;