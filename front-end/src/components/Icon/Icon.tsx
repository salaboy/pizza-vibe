import styles from './Icon.module.css';

type IconPath = { d: string; fillRule?: 'evenodd' | 'nonzero' };

const icons = {
  minus: {
    viewBox: '0 0 24 4',
    width: 24,
    height: 4,
    paths: [{ d: 'M24 0V4H0V0H24Z' }],
  },
  add: {
    viewBox: '0 0 24 24',
    width: 24,
    height: 24,
    paths: [{ d: 'M14 10H24V14H14V24H10V14H0V10H10V0H14V10Z' }],
  },
  delete: {
    viewBox: '0 0 20 20',
    width: 20,
    height: 20,
    paths: [{ d: 'M16 4H20V8H17.5L16 20H4L2.5 8H0V4H4V0H16V4Z' }],
  },
  send: {
    viewBox: '0 0 24.8281 22.8291',
    width: 24.8281,
    height: 22.8291,
    paths: [{ d: 'M24.8281 11L12.999 22.8291L10.1709 20L17.1426 13.0283H0V9.02832H17.1992L11 2.82812L13.8281 0L24.8281 11Z' }],
  },
  check: {
    viewBox: '0 0 26.8701 19.7988',
    width: 26.8701,
    height: 19.7988,
    paths: [{ d: 'M26.8701 2.82812L9.89941 19.7988L0 9.89941L2.82812 7.07129L9.89941 14.1426L24.042 0L26.8701 2.82812Z' }],
  },
  order: {
    viewBox: '0 0 24 25',
    width: 24,
    height: 25,
    paths: [{ d: 'M24 24H20L16.6387 20.4707L12.085 25L7.53223 20.4717L4 24.0039V24H0V0H24V24ZM4 18.3477L7.53223 14.8154L12.085 19.3682L16.6387 14.8145L20 18.3438V4H4V18.3477Z', fillRule: 'evenodd' }],
  },
  kitchen: {
    viewBox: '0 0 24 24',
    width: 24,
    height: 24,
    paths: [
      { d: 'M10 18H6V14H10V18Z' },
      { d: 'M18 18H14V14H18V18Z' },
      { d: 'M24 0V24H0V0H24ZM4 12V20H20V12H4ZM4 4V8H20V4H4Z', fillRule: 'evenodd' },
    ],
  },
  delivery: {
    viewBox: '0 0 28.1172 30',
    width: 28.1172,
    height: 30,
    paths: [{ d: 'M28.1172 27.3867V30H0V27.3799L2.34473 8H6.11719C6.11719 3.58172 9.69891 0 14.1172 0C18.5355 0 22.1172 3.58172 22.1172 8H25.9014L28.1172 27.3867ZM18.1172 8C18.1172 5.79086 16.3263 4 14.1172 4C11.908 4 10.1172 5.79086 10.1172 8H18.1172ZM4.19727 26H23.9336L22.333 12H5.89062L4.19727 26Z', fillRule: 'evenodd' }],
  },
} satisfies Record<string, { viewBox: string; width: number; height: number; paths: IconPath[] }>;

export type IconName = keyof typeof icons;

interface IconProps {
  name: IconName;
  className?: string;
}

export default function Icon({ name, className }: IconProps) {
  const icon = icons[name];

  return (
    <span
      className={`${styles.icon}${className ? ` ${className}` : ''}`}
      role="img"
      aria-label={name}
    >
      <svg
        viewBox={icon.viewBox}
        width={icon.width}
        height={icon.height}
        fill="none"
        xmlns="http://www.w3.org/2000/svg"
      >
        {icon.paths.map((p: IconPath, i) => (
          <path key={i} d={p.d} fill="currentColor" fillRule={p.fillRule} />
        ))}
      </svg>
    </span>
  );
}
