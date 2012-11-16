%% write matlab to file
dlmwrite('../proj_cpp/img/im1_in.txt',im1_in);
dlmwrite('../proj_cpp/img/im2_in.txt',im2_in);

%% read in c++ result
u_cpp = dlmread('../proj_cpp/u.txt');
v_cpp = dlmread('../proj_cpp/v.txt');

u_err = sqrt(sum(sum((u-u_cpp).*(u-u_cpp)))/double(m*n))
v_err = sqrt(sum(sum((v-v_cpp).*(v-v_cpp)))/double(m*n))

%% create error analysis
Fbits = 10:2:20;
u_rms = zeros(size(Fbits));
v_rms = zeros(size(Fbits));

%% load error
i=6;
u_rms(i) = u_err
v_rms(i) = v_err

%% Plot vector image
s = size(u_cpp);
step = max(s / 60);
[X, Y] = meshgrid(1:step:s(2), s(1):-step:1);

u_out = interp2(u_cpp, X, Y);
v_out = interp2(v_cpp, X, Y);

u_out = medfilt2(u_out);
v_out = medfilt2(v_out);
quiver(X, -Y, u_out, -v_out, 1, 'k', 'LineWidth', 1);

%% Plot error analysis

plot(Fbits, u_rms);

plot(Fbits, v_rms);

