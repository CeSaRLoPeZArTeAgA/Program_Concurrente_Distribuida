FROM node:22-alpine
RUN apk add --no-cache postgresql-client curl ca-certificates
WORKDIR /app
COPY worker.js /app/worker.js
EXPOSE 8002
CMD ["node", "worker.js"]
