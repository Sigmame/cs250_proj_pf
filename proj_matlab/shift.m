image_file1='frame10.png';
im1=imread(image_file1);
im1_in=im1(:,:,1);
[m,n] = size(im1_in)
imwrite(im1_in,'a.png','png');
%im2=zeros(m,n);
%im2=im1_in;
for i = 1:m
    if i<30
        im2(i,:)=im1_in(i,:);
    else
        im2(i,:)=im1_in(i-1,:);
    end
end
imshow(im2)
imwrite(im2,'b.png','png')

