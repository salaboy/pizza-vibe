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
  drinks: {
    viewBox: '0 0 23 24',
    width: 23,
    height: 24,
    paths: [{ d: 'M22.8965 3.37891L13.4482 13.2988V20H19.4482V24H3.44824V20H9.44824V13.2988L0 3.37891L1.44824 0H21.4482L22.8965 3.37891ZM11.4482 9.59961L16.7822 4H6.11426L11.4482 9.59961Z', fillRule: 'evenodd' }],
  },
  inventory: {
    viewBox: '0 0 24 29',
    width: 24,
    height: 29,
    paths: [{ d: 'M18.6328 3.79492L14 5.33887V7.85254C17.0059 6.99048 19.5365 7.34881 21.3867 9.12793C23.5216 11.1808 24 14.5297 24 17.2646C23.9998 23.759 18.5561 28.8975 12 28.8975C5.44386 28.8975 0.00023341 23.759 0 17.2646C2.6243e-05 14.5297 0.478406 11.1808 2.61328 9.12793C4.46354 7.34881 6.99406 6.99048 10 7.85254V2.45605L17.3672 0L18.6328 3.79492ZM18.6133 12.0117C18.0265 11.4478 16.5975 10.768 12.8721 12.5723L12 12.9941L11.1279 12.5723C7.40249 10.768 5.97345 11.4478 5.38672 12.0117C4.52169 12.8435 4.00003 14.6796 4 17.2646C4.00024 21.4102 7.51073 24.8975 12 24.8975C16.4893 24.8975 19.9998 21.4102 20 17.2646C20 14.6796 19.4783 12.8435 18.6133 12.0117Z', fillRule: 'evenodd' }],
  },
  bikes: {
    viewBox: '0 0 33 28',
    width: 33,
    height: 28,
    paths: [{ d: 'M6 16C9.31371 16 12 18.6863 12 22C12 25.3137 9.31371 28 6 28C2.68629 28 0 25.3137 0 22C0 18.6863 2.68629 16 6 16ZM27 16C30.3137 16 33 18.6863 33 22C33 25.3137 30.3137 28 27 28C23.6863 28 21 25.3137 21 22C21 18.6863 23.6863 16 27 16ZM6 20C4.89543 20 4 20.8954 4 22C4 23.1046 4.89543 24 6 24C7.10457 24 8 23.1046 8 22C8 20.8954 7.10457 20 6 20ZM27 20C25.8954 20 25 20.8954 25 22C25 23.1046 25.8954 24 27 24C28.1046 24 29 23.1046 29 22C29 20.8954 28.1046 20 27 20ZM21 0C23.2091 0 25 1.79086 25 4C25 6.20914 23.2091 8 21 8C20.6416 8 20.2944 7.95171 19.9639 7.86328L19.8271 7.99902L22.8281 11H26V15H21.1719L16.999 10.8271L14.8281 13L18 16.1719V23.5H14V17.8281L9.17188 13L17.1357 5.03516C17.0475 4.70494 17 4.35803 17 4C17 1.79086 18.7909 0 21 0Z', fillRule: 'evenodd' }],
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
