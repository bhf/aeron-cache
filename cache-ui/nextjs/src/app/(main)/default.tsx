// Fallback for the implicit `children` slot of this parallel-route layout.
// The layout (layout.tsx) renders only the @dashboard, @main and @cacheview
// slots and never renders `children`, but Next.js still requires the children
// slot to resolve for every URL the sibling slots match. Returning null gives
// it a no-op fallback for all nested routes.
export default function Default() {
    return null;
}
