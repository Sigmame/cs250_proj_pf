function [] = plotflow(f, kind)
%PLOTFLOW   Plot flow field
%   PLOTFLOW(F[, KIND]) plots an optical flow field F.  The optional
%   argument KIND specifies the kind of flow field plot:
%    - 'quiver', 'vector': needle-type plot (default)
%    - 'rgb': color plot (blue encodes U, green encodes V)
%    - 'hsv': color plot (hue encodes angle, value encodes velocity)
%    - 'bw': grayscale plot (U on the left, V on the right) 
%    - 'bwscale': also print out the flow scaling
%    - 'mag': grayscale plot of flow magnitude
%    - 'magscale': also print out the flow scaling
%
 
s = size(f);
step = max(s / 40);
    
    [X, Y] = meshgrid(1:step:s(2), s(1):-step:1);
    u = interp2(f(:, :, 1), X, Y);
    v = interp2(f(:, :, 2), X, Y);
    quiver(X, -Y, u, -v, 1, 'k', 'LineWidth', 1);
    axis image;
  end