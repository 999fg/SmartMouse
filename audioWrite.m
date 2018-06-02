Fs = 44100;
freq = 19000;
values=0:1/Fs:100;
a = 100*sin(2*pi*freq*values);
b = fft(a);

soundsc(a, Fs, 16);

% 
% filename = '17khz.wav';
% audiowrite(filename, y, Fs);
% clear y Fs