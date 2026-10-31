FROM node:20.10-alpine AS base
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm install
COPY .. .
RUN npm run build

FROM node:20.10-alpine as release
WORKDIR /app
ENV NODE_ENV production

# The release version (e.g. 0.0.41-SNAPSHOT), passed in from CI and read at runtime by
# /api/version to compare against the latest GitHub release. Defaults to "dev" for local builds.
ARG APP_VERSION=dev
ENV APP_VERSION=${APP_VERSION}

RUN addgroup --system --gid 1001 nodejs
RUN adduser --system --uid 1001 nextjs
COPY --from=base /app/public ./public

RUN mkdir .next
RUN chown nextjs:nodejs .next

COPY --from=base --chown=nextjs:nodejs /app/.next/standalone ./
COPY --from=base --chown=nextjs:nodejs /app/.next/static ./.next/static

USER nextjs
EXPOSE 3000
ENV PORT=3000
CMD HOSTNAME="0.0.0.0" node server.js