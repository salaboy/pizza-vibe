import Logo from '@/components/Logo';
import styles from './Footer.module.css';

export default function Footer() {
  return (
    <footer className={styles.footer}>
      <Logo size="small" />
      <div className={styles.info}>
        <span>PizzaVibe V1.0</span>
        <a href="https://github.com" target="_blank" rel="noopener noreferrer">
          GitHub
        </a>
      </div>
    </footer>
  );
}
