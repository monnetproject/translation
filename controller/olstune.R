olstune <- function(x) {
  N <- max(x[,ncol(x)])
  n <- rep(0,ncol(x)-2)
  y <- rep(0,ncol(x)-2)
  for(i in 1:N) {
     if(!all(x[x[,ncol(x)]==i,ncol(x)-1] == 0)) {
     y2 <- c()
     for(j in 1:(ncol(x)-2)) {
       z <- lm(x[x[,ncol(x)]==i,ncol(x)-1]~x[x[,ncol(x)]==i,j])$coefficients[2]
       y2 <- c(y2,z)
     }
       n <- n + !is.na(y2)
     y2[is.na(y2)] <- 0
     #if(all(!is.na(y2))) {
       y <- y + y2
     #}
  }
}
  y / n
}
