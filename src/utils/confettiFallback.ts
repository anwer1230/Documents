export default function confetti(options?: any) {
  // Graceful no-op fallback when canvas-confetti is not bundled
  return Promise.resolve();
}
