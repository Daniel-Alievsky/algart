my @list = ();
my $size = 64*1024*1024;
my $sum = 0;
for ($k = 0; $k < 2000; $k++) {
  my $a = '';
  vec($a,$size-1,8) = ' ';
  $sum += $size;
  push @list,$a;
  undef $a;
  printf($sum/(1024*1024) . " Mb allocated\n");    
}