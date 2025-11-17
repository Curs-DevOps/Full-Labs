FROM nginx:alpine

# Set working directory to the default nginx html folder
WORKDIR /usr/share/nginx/html

# Remove default nginx content
RUN rm -rf ./*

# Copy static frontend files into the container
COPY index.html .
COPY dashboard.css .
COPY dashboard.js .

# Expose port 80 inside the container
EXPOSE 80

# Start nginx in the foreground
CMD ["nginx", "-g", "daemon off;"]
