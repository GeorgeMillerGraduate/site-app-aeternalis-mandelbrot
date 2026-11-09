#pragma OPENCL EXTENSION cl_khr_fp64 : enable
#pragma OPENCL FP_CONTRACT OFF
// Independent samples. No float downgrade, fast-relaxed-math or flush-to-zero option is used.
__kernel void escape_fp64(__global double *values,__global int *iterations,__global int *reasons,
  int total,int x0,int y0,int tw,int imageWidth,int imageHeight,int grid,int limit,
  double centreR,double centreI,double span,double threshold,int julia,double constantR,double constantI) {
  int at=get_global_id(0);if(at>=total)return;int spp=grid*grid,pixel=at/spp,sub=at%spp;
  double px=x0+pixel%tw+(sub%grid+0.5)/grid,py=y0+pixel/tw+(sub/grid+0.5)/grid;
  double r=centreR+(px-imageWidth/2.0)/imageWidth*span,i=centreI-(py-imageHeight/2.0)/imageWidth*span;
  double a=julia?r:0,b=julia?i:0,cr=julia?constantR:r,ci=julia?constantI:i;int n=0;
  if(!julia){double yy=i*i,q=(r-.25)*(r-.25)+yy;
    if(q*(q+r-.25)<.25*yy-1e-14){values[at]=NAN;iterations[at]=0;reasons[at]=1;return;}
    if((r+1)*(r+1)+yy<.0625-1e-14){values[at]=NAN;iterations[at]=0;reasons[at]=2;return;}}
  while(a*a+b*b<=threshold&&n<limit){double next=a*a-b*b+cr;b=2*a*b+ci;a=next;n++;}
  double norm=a*a+b*b;iterations[at]=n;
  if(fabs(norm-threshold)<1e-10*(norm+threshold)){reasons[at]=-1;values[at]=NAN;return;}
  if(norm>threshold){int refined=n;for(int k=0;k<3;k++){double next=a*a-b*b+cr,bi=2*a*b+ci;if(!isfinite(next)||!isfinite(bi))break;a=next;b=bi;refined++;}
    values[at]=refined+1-log(log(hypot(a,b)))/log(2.0);reasons[at]=0;
  }else{values[at]=NAN;reasons[at]=4;}
}
