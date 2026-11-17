/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    "./docs/**/*.{html,js}",
    "./poc-camara/**/*.{html,js}"
  ],
  theme: {
    extend: {
      colors: {
        salgueros: '#BB2528',
      }
    },
  },
  plugins: [],
}
