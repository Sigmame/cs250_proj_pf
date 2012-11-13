%% write matlab to file
dlmwrite('../proj_cpp/img/im1_in.txt',im1_in);
dlmwrite('../proj_cpp/img/im2_in.txt',im2_in);

%% read in c++ result
u_cpp = dlmread('../proj_cpp/u.txt');
v_cpp = dlmread('../proj_cpp/v.txt');

sqrt(sum(sum((u-u_cpp).*(u-u_cpp)))/double(m*n))
sqrt(sum(sum((v-v_cpp).*(v-v_cpp)))/double(m*n))
