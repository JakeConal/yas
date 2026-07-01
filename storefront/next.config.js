const apiBasePath = (process.env.API_BASE_PATH || 'http://storefront.yas.local.com/api').replace(
  /\/$/,
  ''
);
const bffBasePath = apiBasePath.replace(/\/api$/, '');

/** @type {import('next').NextConfig} */
const nextConfig = {
  reactStrictMode: true,
  output: 'standalone',
  images: {
    remotePatterns: [
      {
        hostname: 'api.yas.local',
      },
    ],
  },
  async rewrites() {
    return [
      {
        source: '/api/:path*',
        destination: `${apiBasePath}/:path*`,
      },
      {
        source: '/authentication',
        destination: `${bffBasePath}/authentication`,
      },
      {
        source: '/oauth2/:path*',
        destination: `${bffBasePath}/oauth2/:path*`,
      },
      {
        source: '/login/oauth2/:path*',
        destination: `${bffBasePath}/login/oauth2/:path*`,
      },
    ];
  },
};

module.exports = nextConfig;
