image_file1='a.png';
image_file2='b.png';
im1_in=imread(image_file1);
im2_in=imread(image_file2);
im1=double(im1_in);
im2=double(im2_in);
[m,n] = size(im1)
[x,y] = size(im2)
tic
for i = 1:m-1
    for j = 1:n-1
        %calculate X gradient
        Ex_1 = im1(i,j+1)-im1(i,j)+im1(i+1,j+1)-im1(i+1,j);
        Ex_2 = im2(i,j+1)-im2(i,j)+im2(i+1,j+1)-im2(i+1,j);
        Ex(i,j) = (Ex_1 + Ex_2)/4;
        %calculate Y gradient
        Ey_1 = im1(i+1,j)-im1(i,j)+im1(i+1,j+1)-im1(i,j+1);
        Ey_2 = im2(i+1,j)-im2(i,j)+im2(i+1,j+1)-im2(i,j+1);
        Ey(i,j) = (Ey_1 + Ey_2)/4;
        %calculate T gradient
        Et_1 = im2(i,j)-im1(i,j)+im2(i+1,j)-im1(i+1,j);
        Et_2 = im2(i,j+1)-im1(i,j+1)+im2(i+1,j+1)-im1(i+1,j+1);
        Et(i,j) = (Et_1 + Et_2)/4;
    end
end

u = zeros(size(im1));
v = zeros(size(im1));
for i = 1:m-1
    for j = 1:n-1
        D(i,j)=0.01+Ex(i,j)^2+Ey(i,j)^2;
    end
end

for k = 1:500  %number of interation
    for i = 2:m-1
        for j = 2:n-1
            %average u vector
            u_avg_1 = u(i-1,j)+u(i,j+1)+u(i+1,j)+u(i,j-1);
            u_avg_2 = u(i-1,j-1)+u(i-1,j+1)+u(i+1,j+1)+u(i+1,j-1);
            u_avg(i,j) = u_avg_1/6 + u_avg_2/12;
            %average v vector
            v_avg_1 = v(i-1,j)+v(i,j+1)+v(i+1,j)+v(i,j-1);
            v_avg_2 = v(i-1,j-1)+v(i-1,j+1)+v(i+1,j+1)+v(i+1,j-1);
            v_avg(i,j) = v_avg_1/6 + v_avg_2/12;
        end
    end

 
    %calculate P term    
    for i = 1:m-1
        for j = 1:n-1
            P(i,j)=Ex(i,j)*u_avg(i,j)+Ey(i,j)*v_avg(i,j)+Et(i,j);
        end
    end 
    
    %calculate u and v
    for i = 2:m-1
        for j = 2:n-1
            u(i,j) = u_avg(i,j)-P(i,j)*Ex(i,j)/D(i,j);
            v(i,j) = v_avg(i,j)-P(i,j)*Ey(i,j)/D(i,j);
        end
    end
    
end

s = size(im1);
step = max(s / 60);
[X, Y] = meshgrid(1:step:s(2), s(1):-step:1);
u_out = interp2(u, X, Y);
v_out = interp2(v, X, Y);
%imshow(u)
quiver(u_out,v_out)
toc