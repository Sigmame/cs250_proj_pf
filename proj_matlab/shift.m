image_file1='frame10.png';
image_file2='frame11.png';
im1=imread(image_file1);
im3=imread(image_file2);
im1_in=im1(:,:,1);
im3_in=im3(:,:,1);
[m,n] = size(im1_in)
imwrite(im1_in,'v1.png','png');
imwrite(im3_in,'o2.png','png');
%im2=zeros(m,n);
%im2=im1_in;

%for j = 1:n
%    if j<30
%        im2(:,j)=im1_in(:,j);
%    else
%        im2(:,j)=im1_in(:,j-5);
%    end
%end

for i = 2:m
    for j= 2:n
        im2(i,j)=im1_in(i-1,j-1);
    end
end
imshow(im2)
imwrite(im2,'diagonal.png','png')

